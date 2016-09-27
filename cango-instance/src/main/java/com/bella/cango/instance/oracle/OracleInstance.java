package com.bella.cango.instance.oracle;

import com.bella.cango.enums.DbType;
import com.bella.cango.instance.oracle.applier.KafkaRecordApplier;
import com.bella.cango.instance.oracle.applier.RecordApplier;
import com.bella.cango.instance.oracle.common.YuGongConstants;
import com.bella.cango.instance.oracle.common.db.DataSourceFactory;
import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.bella.cango.instance.oracle.common.db.meta.Table;
import com.bella.cango.instance.oracle.common.db.meta.TableMetaGenerator;
import com.bella.cango.instance.oracle.common.lifecycle.AbstractYuGongLifeCycle;
import com.bella.cango.instance.oracle.common.model.DataSourceConfig;
import com.bella.cango.instance.oracle.common.model.ExtractStatus;
import com.bella.cango.instance.oracle.common.model.RunMode;
import com.bella.cango.instance.oracle.common.model.YuGongContext;
import com.bella.cango.instance.oracle.common.model.record.Record;
import com.bella.cango.instance.oracle.common.utils.LikeUtil;
import com.bella.cango.instance.oracle.common.utils.YuGongUtils;
import com.bella.cango.instance.oracle.common.utils.thread.NamedThreadFactory;
import com.bella.cango.instance.oracle.exception.YuGongException;
import com.bella.cango.instance.oracle.extractor.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/7
 */
public class OracleInstance extends AbstractYuGongLifeCycle {
    private static Logger logger = LoggerFactory.getLogger(OracleInstance.class);

    private final Map<String, Object> params;
    private final RunMode runMode = RunMode.ALL;
    private final DbType sourceDbType = DbType.ORACLE;
    private final int noUpdateThreshold = 3;
    private final int retryTimes = 3;
    private final int retryInterval = 1000;
    private final int threadSize;
    private final AtomicInteger count;  // 对添加table计数，均匀hash到tableHolderMap
    private final Semaphore[] semaphores;
    private final Map<String, Set<TableHolder>> tableHolderMap;
    private YuGongContext globalContext;
    private DataSourceFactory dataSourceFactory = new DataSourceFactory();
    private String name;   // 实例名称
    private volatile boolean extractorDump = true;
    private volatile boolean applierDump = false;
    private YuGongException exception = null;
    private RecordExtractor extractor;
    private RecordApplier applier;
    private String executorName;
    private ThreadPoolExecutor threadPoolExecutor;

    private OracleInstance() {
        this(new HashMap(), null);
        throw new UnsupportedOperationException();
    }

    public OracleInstance(Map params, String name) {
        this.params = params;
        this.name = name;
        this.count = new AtomicInteger(0);
        this.threadSize = 1;
//        this.threadSize = Runtime.getRuntime().availableProcessors();
        this.tableHolderMap = new ConcurrentHashMap<String, Set<TableHolder>>() {
            {
                for (int i = 0; i < threadSize; i++) {
                    put(String.valueOf(i), new ConcurrentSkipListSet<TableHolder>());
                }
            }
        };
        this.semaphores = new Semaphore[threadSize];
        Arrays.fill(semaphores, new Semaphore(0));
    }

    public void start() {
        super.start();

        try {
            if (!dataSourceFactory.isStart()) {
                dataSourceFactory.start();
            }

            globalContext = initGlobalContext();
            extractor = initExtractor(globalContext, runMode);
            applier = initApplier(globalContext);

            if (!extractor.isStart()) {
                extractor.start();
            }

            if (!applier.isStart()) {
                applier.start();
            }

            // 1.将同步的表哈希到tableHolderMap不同的value中
            // 2.开启一个线程池，分别遍历不同key下的tableHolderMap
            executorName = this.getClass().getSimpleName() + "-" + globalContext.getName() + "-" + globalContext.getDbName();
            threadPoolExecutor = new ThreadPoolExecutor(threadSize,
                    threadSize,
                    60,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(threadSize * 2),
                    new NamedThreadFactory(executorName), new ThreadPoolExecutor.CallerRunsPolicy());
            for (int i = 0; i < threadSize; i++) {
                final int temp = i;
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String key = String.valueOf(temp);
                        label:
                        while (true) {
                            try {
                                Set<TableHolder> tableHolders = tableHolderMap.get(key);
                                if (CollectionUtils.isEmpty(tableHolders)) {
                                    if (OracleInstance.this.isStop()) {
                                        break label;
                                    }
                                    semaphores[temp].acquire();

                                    if (OracleInstance.this.isStop()) {
                                        break label;
                                    }
                                }

                                for (TableHolder tableHolder : tableHolders) {
                                    if (OracleInstance.this.isStop()) {
                                        break label;
                                    }

                                    try {
                                        Table tableMeta = tableHolder.table;
                                        processTable(globalContext, tableMeta, extractor, applier);
                                    } catch (Throwable e) {
                                        if (OracleInstance.this.isStop()) {
                                            logger.info("shutdown OracleInstance thread:[{}]", Thread.currentThread().getName());
                                            break label;
                                        } else {
                                            logger.error("synchronize table[{}] happened exception. caused by {}", tableHolder.table.getName(),
                                                    ExceptionUtils.getFullStackTrace(e));
                                        }
                                    }
                                }

                                Thread.sleep(retryInterval);
                            } catch (InterruptedException e) {
                                logger.info("shutdown OracleInstance thread:[{}], end the while loop", Thread.currentThread().getName());
                                break label;
                            } catch (Throwable e) {
                                logger.error("OracleInstance happened exception. caused by {}", ExceptionUtils.getFullStackTrace(e));
                            }
                        }
                    }
                });
            }
        } catch (Throwable e) {
            logger.error("OracleInstance[{}] start failed caused by {}",
                    name,
                    ExceptionUtils.getFullStackTrace(e));
        }
    }

    /**
     * 暂时的方案是处理每张表到没有数据
     *
     * @param context
     * @param table
     * @param extractor
     * @param applier
     */
    private void processTable(YuGongContext context, Table table, RecordExtractor extractor, RecordApplier applier) {
        try {
            MDC.put(YuGongConstants.MDC_TABLE_SHIT_KEY, table.getFullName());
            ExtractStatus status = ExtractStatus.NORMAL;
//            AtomicLong batchId = new AtomicLong(0);
            long tpsLimit = context.getTpsLimit();
            int noUpdateTimes = 0;
            do {
                long start = System.currentTimeMillis();
                // 提取数据
                Map<String, Object> rowIdsAndRecords = extractor.extract(table);
                String mlogCleanSql = (String) rowIdsAndRecords.get("mlogCleanSql");
                List<ColumnValue> rowsIds = (List<ColumnValue>) rowIdsAndRecords.get("rowIds");
                List<Record> records = (List<Record>) rowIdsAndRecords.get("kafkaIncrementRecords");

                List<Record> ackRecords = records;// 保留ack引用


                // 判断是否记录日志
//                RecordDumper.dumpExtractorInfo(batchId.incrementAndGet(),
//                        ackRecords,
//                        extractorDump);

                if (YuGongUtils.isEmpty(records)) {
                    status = extractor.status();
                } else {
                    // 载入数据
                    Throwable applierException = null;
                    for (int i = 0; i < retryTimes; i++) {
                        try {
                            applier.apply(records, table);
                            applierException = null;
                            break;
                        } catch (Throwable e) {
                            applierException = e;
                            if (processException(context, table, extractor, e)) {
                                break;
                            }
                        }
                    }

                    if (applierException != null) {
                        throw applierException;
                    } else { // 如果extractor,applier整个过程无误，就可以清理MLOG$_{TABLE}中的数据
                        extractor.clearMlog(rowsIds, mlogCleanSql);
                    }
                }

                // 判断是否记录日志
//                RecordDumper.dumpApplierInfo(batchId.get(), ackRecords, records, applierDump);

                long end = System.currentTimeMillis();

                if (tpsLimit > 0) {
                    tpsControl(ackRecords, start, end, tpsLimit);
                }

                Thread.sleep(retryInterval);
                // 控制一下增量的退出
                if (status == ExtractStatus.NO_UPDATE) {
                    noUpdateTimes++;
                    if (noUpdateThreshold > 0 && noUpdateTimes > noUpdateThreshold) {
                        break;
                    }
                }
            } while (status != ExtractStatus.TABLE_END);
            table.getColumns().clear();
            table.getPrimaryKeys().clear();
            logger.info("table[{}] is end by {}", table.getFullName(), status);
            table = null;
        } catch (InterruptedException e) {
            throw new YuGongException(e);
        } catch (Throwable e) {
            throw new YuGongException(e);
        }
    }

    private boolean processException(YuGongContext context, Table table, RecordExtractor extractor, Throwable e) {
        if (ExceptionUtils.getRootCause(e) instanceof InterruptedException) {
            // interrupt事件，响应退出
            logger.info("table[{}] is interrpt ,current status:{} !", table
                    .getFullName(), extractor.status());
            return true;
        } else if (OracleInstance.this.isStop()) {
            return true;
        } else {
            logger.error("retry, something error happened. caused by {}",
                    ExceptionUtils.getFullStackTrace(e));
            logger.info("table[{}] is error , current status:{} !", table
                    .getFullName(), extractor.status());
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e1) {
                exception = new YuGongException(e1);
                Thread.currentThread().interrupt();
                return true;
            }
        }
        return false;
    }


    private RecordApplier initApplier(YuGongContext context) {
        return new KafkaRecordApplier(context);
    }

    private RecordExtractor initExtractor(YuGongContext context, RunMode runMode) {
        if (context.getSourceDbType() == DbType.ORACLE) {
            if (runMode == RunMode.MARK) {
                return new KafkaRecRecordExtractor(context);
            } else if (runMode == RunMode.INC) {
                KafkaIncRecordExtractor recordExtractor = new KafkaIncRecordExtractor(context);
                recordExtractor.setSleepTime(1000L);
                return recordExtractor;
            } else {
                AbstractRecordExtractor incExtractor = (AbstractRecordExtractor) initExtractor(
                        context,
                        RunMode.INC);
                KafkaAllRecordExtractor allExtractor = new KafkaAllRecordExtractor(context);
                allExtractor.setKafkaIncExtractor((AbstractOracleRecordExtractor) incExtractor);
                return allExtractor;
            }
        } else {
            throw new YuGongException("Unsupported " + sourceDbType);
        }
    }

    private YuGongContext initGlobalContext() {
        YuGongContext context = new YuGongContext();
        logger.info("check source database connection ...");
        context.setSourceDs(initDataSource());
        logger.info("check source database is ok");
        context.setSourceEncoding("UTF-8");
        context.setBatchApply(true);
        context.setOnceCrawNum(200);
        context.setTpsLimit(2000);
        context.setIgnoreSchema(true);
        context.setSkipApplierException(false);
        context.setRunMode(runMode);

        // 手动添加的参数
        context.setName((String) params.get("instance_name"));
        context.setDbName((String) params.get("db_name"));
        context.setDbHost((String) params.get("db_host"));
        context.setDbPort((Integer) params.get("db_port"));
        return context;
    }

    private DataSource initDataSource() {
        String username = (String) params.get("db_username");
        String password = (String) params.get("db_password");
        String url = (String) params.get("db_url");
        DbType dbType = DbType.ORACLE;
        String poolSize = "30";

        Properties properties = new Properties();
        if (poolSize != null) {
            properties.setProperty("maxActive", poolSize);
        } else {
            properties.setProperty("maxActive", "200");
        }

        DataSourceConfig dsConfig = new DataSourceConfig(url, username, password, dbType, properties);
        return dataSourceFactory.getDataSource(dsConfig);
    }

    private List<TableHolder> initTable(String tableName) {
        logger.info("check source tables read privileges ...");
        String[] strArr = StringUtils.split(tableName, ".");
        List<Table> whiteTables = null;
        List<String> blackTables = (List<String>) params.get("black_tables");
        boolean ignoreSchema = false;

        if (strArr.length == 2 && !isBlackTable(tableName, blackTables)) {
            if (tableName.indexOf("*") == -1) {  // 某个表
                whiteTables = TableMetaGenerator.getTableMetasWithoutColumn(globalContext.getSourceDs(),
                        strArr[0],
                        strArr[1]);
            } else {                             // 某个库
                whiteTables = TableMetaGenerator.getTableMetasWithoutColumn(globalContext.getSourceDs(),
                        strArr[0],
                        null);
            }

        } else {
            throw new YuGongException("table[" + tableName + "] is not valid");
        }

        if (whiteTables.isEmpty()) {
            throw new YuGongException("table[" + tableName + "] is not found");
        }

        List<TableHolder> tableHolders = Lists.newArrayList();
        for (Table table : whiteTables) {
            // 根据实际表名处理一下
            if (!isBlackTable(table.getName(), blackTables)) {
                TableMetaGenerator.buildColumns(globalContext.getSourceDs(), table);
                TableHolder holder = new TableHolder(table);
                holder.ignoreSchema = ignoreSchema;
                tableHolders.add(holder);
            }
        }

        logger.info("check source tables is ok.");

        return tableHolders;
    }

    private boolean isBlackTable(String table, List tableBlackList) {
        for (Object tableBlack : tableBlackList) {
            if (LikeUtil.isMatch((String) tableBlack, table)) {
                return true;
            }
        }

        return false;
    }

    private boolean addTableName(String tableName) {
        if (checkTableName(tableName)) {
            int hash = count.getAndIncrement();
            hash %= threadSize;
            Set<TableHolder> tableHolders = tableHolderMap.get(String.valueOf(hash));
            boolean result = tableHolders.addAll(initTable(tableName));

            if (!result) { // 如果添加失败，count就减一还原
                count.decrementAndGet();
            }

            if (result && semaphores[hash].availablePermits() == 0) {
                semaphores[hash].release();
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * 除非全部的tableNames均为空，返回false，否则为true
     *
     * @param tableNames
     * @return 返回新添加的表
     */
    public synchronized List<String> addTableName(List<String> tableNames) {
        if (CollectionUtils.isEmpty(tableNames)) {
            return null;
        } else {
            List<String> addedTables = new ArrayList<>();
            for (int i = 0, len = tableNames.size(); i < len; i++) {
                String table = tableNames.get(i);
                if (addTableName(table)) {
                    addedTables.add(table);
                }
            }
            return addedTables;
        }
    }

    private boolean checkTableName(String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            logger.warn("tableName should not be null!");
            return false;
        }

        if (!tableName.contains(".")) {
            logger.warn("schema should not be null, please set schema for table[" + tableName + "]");
            return false;
        }

        for (int i = 0; i < threadSize; i++) {
            for (TableHolder tableHolder : tableHolderMap.get(String.valueOf(i))) {
                // 忽略区分大小写
                if (tableHolder.table.getName().equalsIgnoreCase(tableName)) {
                    return false;
                }
            }
        }

        return true;
    }


    public void stop() {
        super.stop();
        if (!extractor.isStop()) {
            extractor.stop();
        }

        if (!applier.isStop()) {
            applier.stop();
        }

        threadPoolExecutor.shutdownNow();

        releaseSemaphores(semaphores);

        while (!threadPoolExecutor.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (dataSourceFactory.isStart()) {
            dataSourceFactory.stop();
        }

        exception = null;

        tableHolderMap.clear();
        logger.info("OracleInstance [{}] stop successful. ", name);
    }

    private void releaseSemaphores(Semaphore[] semaphores) {
        for (Semaphore semaphore : semaphores) {
            semaphore.release();
        }
    }

    private void tpsControl(List<Record> result, long start, long end, long tps) throws InterruptedException {
        long expectTime = (result.size() * 1000) / tps;
        long runTime = expectTime - (end - start);
        if (runTime > 0) {
            Thread.sleep(runTime);
        }
    }

    public String getName() {
        return name;
    }

    private static class TableHolder {
        Table table;
        boolean ignoreSchema = false;

        public TableHolder(Table table) {
            this.table = table;
        }
    }
}
