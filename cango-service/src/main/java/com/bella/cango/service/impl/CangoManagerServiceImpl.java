package com.bella.cango.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.instance.manager.model.Canal;
import com.alibaba.otter.canal.instance.manager.model.CanalParameter;
import com.bella.cango.dto.CangoRequestDto;
import com.bella.cango.dto.CangoResponseDto;
import com.bella.cango.entity.CangoInstances;
import com.bella.cango.entity.CangoTable;
import com.bella.cango.enums.CangoRspStatus;
import com.bella.cango.enums.DbType;
import com.bella.cango.enums.OracleParamEnum;
import com.bella.cango.enums.State;
import com.bella.cango.instance.cache.CangoInstanceCache;
import com.bella.cango.instance.mysql.MysqlInstance;
import com.bella.cango.instance.oracle.OracleInstance;
import com.bella.cango.instance.oracle.exception.YuGongException;
import com.bella.cango.service.CangoInstancesService;
import com.bella.cango.service.CangoManagerService;
import com.bella.cango.service.CangoTableService;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.*;


@Service("cangoManagerService")
public class CangoManagerServiceImpl implements CangoManagerService, ApplicationListener {
    //日志
    private static final Logger LOGGER = LoggerFactory.getLogger(CangoManagerServiceImpl.class);

    @Autowired
    private String zkCluster;

    private static final String CANAL_INSTANCE = MysqlInstance.class.getSimpleName();

    private static final String ORACLE_INSTANCE = OracleInstance.class.getSimpleName();

    @Autowired
    private CangoInstancesService cangoInstancesService;

    @Autowired
    private CangoTableService cangoTableNameService;

    @Override
    public CangoResponseDto add(CangoRequestDto canalReqDto) {
        canalReqDto.setName(CangoInstanceCache.createKey(canalReqDto.getHost(), canalReqDto.getPort()));
        //创建对象
        CangoInstances cangoInstances = new CangoInstances();
        BeanUtils.copyProperties(canalReqDto, cangoInstances);

        //手动设置感兴趣的表，属性拷贝不行
        if (!CollectionUtils.isEmpty(canalReqDto.getTableNames())) {
            cangoInstances.setTableNames(Lists.newArrayList(canalReqDto.getTableNames()));
        }

        //数据库类型设置
        cangoInstances.setDbType(canalReqDto.getDbType() != null ? canalReqDto.getDbType().getCode() : DbType.MYSQL.getCode());

        //校验是否已经添加过
        CangoInstances cangoInstancesPo = cangoInstancesService.findByName(cangoInstances);
        if (cangoInstancesPo != null && State.DISABLE.getCode().equals(cangoInstancesPo.getState())) {
            CangoResponseDto cangoResponseDto = new CangoResponseDto();
            cangoResponseDto.setStatus(CangoRspStatus.FAILURE);
            cangoResponseDto.setFailMsg(String.format("CangoInstance(%s)has been disabled!", cangoInstances.getName()));
            return cangoResponseDto;
        }

        return doAdd(cangoInstances);
    }

    /**
     * Do add cango response dto.
     *
     * @param cangoInstances the cango instances
     * @return the cango response dto
     */
    private synchronized CangoResponseDto doAdd(CangoInstances cangoInstances) {
        CangoResponseDto CangoResultDto = new CangoResponseDto();

        if (DbType.ORACLE.getCode().equals(cangoInstances.getDbType())) {
            startYuGong(cangoInstances);
        } else {
            //启动canal
            startCanal(cangoInstances);
        }


        //持久化
        cangoInstances.setCreateTime(new Date());
        cangoInstances.setState(State.START.getCode());
        cangoInstancesService.saveOrUpdate(cangoInstances);

        //成功响应
        CangoResultDto.setStatus(CangoRspStatus.SUCCESS);
        return CangoResultDto;

    }

    /**
     * Start canal.
     *
     * @param cangoInstances the cango instances
     */
    private void startCanal(CangoInstances cangoInstances) {
        // 查看该数据库实例是否已启用
        String key = getKey(cangoInstances);

        // 该数据库实例是否已经添加过
        MysqlInstance mysqlInstance = CangoInstanceCache.getMysqlInstance(key);
        if (mysqlInstance == null) {
            // 启动state为STOP的cangoInstance
            CangoInstances cangoInstancesPo = cangoInstancesService.findByName(cangoInstances);
            if (cangoInstancesPo != null) {
                if (State.STOP.getCode().equals(cangoInstancesPo.getState())) {
                    CangoTable cangoTable = new CangoTable();
                    cangoTable.setInstancesName(key);
                    List<CangoTable> cangoTablePos = cangoTableNameService.findByInstancesName(cangoTable);
                    mysqlInstance = buildMysqlInstance(cangoInstances);
                    // 将CanalInstance添加到本地缓存中
                    CangoInstanceCache.addMysqlInstance(key, mysqlInstance);
                    mysqlInstance.addTables(getStringTableName(cangoTablePos));
                }
            } else {
                mysqlInstance = buildMysqlInstance(cangoInstances);
                // 将CanalInstance添加到本地缓存中
                CangoInstanceCache.addMysqlInstance(key, mysqlInstance);
            }
        }

        // 校验注册的表是否合法
        Set<String> filteredTables = filterTableList(cangoInstances.getTableNames());
        // 获取尚未添加的表
        Set<String> newAddedTables = getNewAddedTables(filteredTables, mysqlInstance.getFullTableList());

        mysqlInstance.addTables(filteredTables);
        // 持久化新增的表
        saveNewTables(newAddedTables, key);

        mysqlInstance.start();
    }

    /**
     * Build mysql instance mysql instance.
     *
     * @param cangoInstances the cango instances
     * @return the mysql instance
     */
    private MysqlInstance buildMysqlInstance(CangoInstances cangoInstances) {
        //创建CanalInstance
        CanalParameter canalParameter = buildCanalParameter(cangoInstances);
        Canal canal = new Canal();
        canal.setName(getKey(cangoInstances));
        canal.setCanalParameter(canalParameter);
        return new MysqlInstance(canal);
    }

    /**
     * Gets new added tables.
     *
     * @param filteredTables the filtered tables
     * @param fullTableList  the full table list
     * @return the new added tables
     */
    private Set<String> getNewAddedTables(Set<String> filteredTables, Set<String> fullTableList) {
        if (!CollectionUtils.isEmpty(filteredTables) && !CollectionUtils.isEmpty(fullTableList)) {
            // 解除引用共享
            HashSet<String> newAddedTables = new HashSet<>(filteredTables);
            for (String filteredTable : filteredTables) {
                for (String fullTable : fullTableList) {
                    // 考虑大小写
                    if (filteredTable.equalsIgnoreCase(fullTable)) {
                        newAddedTables.remove(filteredTable);
                    }
                }
            }
            return newAddedTables;
        }
        return null;
    }

    private List<String> getStringTableName(List<CangoTable> cangoTablePos) {
        if (!CollectionUtils.isEmpty(cangoTablePos)) {
            List<String> tableNames = new ArrayList<>();
            for (CangoTable cangoTablePo : cangoTablePos) {
                tableNames.add(cangoTablePo.getTableName());
            }
            return tableNames;
        }
        return null;
    }

    /**
     * Filter table list set.
     * note: 必须满足schema.table的格式
     *
     * @param tableNames the table names
     * @return the set
     */
    private Set<String> filterTableList(List<String> tableNames) {
        if (!CollectionUtils.isEmpty(tableNames)) {
            Set<String> filteredTables = new HashSet<>();
            for (String tableName : tableNames) {
                if (StringUtils.isNotBlank(tableName)
                        && tableName.contains(".")
                        && !tableName.startsWith(".")
                        && !tableName.endsWith(".")
                        && tableName.indexOf(".") == tableName.lastIndexOf(".")) {
                    filteredTables.add(tableName);
                }
            }

            return filteredTables;
        } else {
            return null;
        }
    }

    /**
     * Build canal parameter canal parameter.
     *
     * @param cangoInstances the canal instances
     * @return the canal parameter
     * @date :2016-04-26 17:20:18
     */
    private CanalParameter buildCanalParameter(CangoInstances cangoInstances) {
        CanalParameter parameter = new CanalParameter();
        parameter.setMetaMode(CanalParameter.MetaMode.ZOOKEEPER);
        parameter.setHaMode(CanalParameter.HAMode.HEARTBEAT);
        parameter.setIndexMode(CanalParameter.IndexMode.ZOOKEEPER);

        parameter.setMemoryStorageBufferSize(32 * 1024);

        parameter.setSourcingType(CanalParameter.SourcingType.MYSQL);
        parameter.setDbAddresses(Collections.singletonList(new InetSocketAddress(cangoInstances.getHost(), cangoInstances.getPort() != null ? cangoInstances.getPort() : 3306)));
        parameter.setDbUsername(cangoInstances.getUserName());
        parameter.setDbPassword(cangoInstances.getPassword());

        parameter.setSlaveId(Long.valueOf(cangoInstances.getSlaveId()));

        //zk设置
        parameter.setZkClusters(Arrays.asList(zkCluster.split(",")));

        parameter.setDefaultConnectionTimeoutInSeconds(30);
        parameter.setConnectionCharset("UTF-8");
        parameter.setConnectionCharsetNumber((byte) 33);
        parameter.setReceiveBufferSize(8 * 1024);
        parameter.setSendBufferSize(8 * 1024);

        //过滤掉库
//        parameter.setBlackFilter("xx\\..*");
        parameter.setBlackFilter(blackFilterConverter(cangoInstances.getBlackTables()));

        parameter.setDetectingEnable(false);
        parameter.setFilterTableError(true);
        parameter.setDetectingIntervalInSeconds(10);
        parameter.setDetectingRetryTimes(3);
        parameter.setDetectingSQL("SELECT 'x' FROM DUAL");

        return parameter;
    }

    /**
     * Black filter converter string.
     *
     * @param blackTables the black tables
     * @return the string
     */
    private String blackFilterConverter(String blackTables) {
        if (StringUtils.isNotBlank(blackTables)) {
            List<String> blackList = Arrays.asList(blackTables.split(","));
            Set<String> filterTableList = filterTableList(blackList);
            StringBuilder stringBuilder = new StringBuilder();
            for (String black : filterTableList) {
                if (black.contains("*")) {
                    String schema = black.substring(0, black.indexOf("."));
                    stringBuilder.append(schema).append("\\..*").append(",");
                } else {
                    String[] split = black.split("\\.");
                    stringBuilder.append(split[0]).append("\\.").append(split[1]).append(",");
                }
            }

            return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
        } else {
            return "";
        }
    }

    /**
     * Start yu gong.
     *
     * @param cangoInstances the canal instances
     * @date : 2016-07-05 14:05:30
     */
    private void startYuGong(CangoInstances cangoInstances) {
        //查看该数据库实例是否已启用
        String key = getKey(cangoInstances);

        //该数据库实例是否已经添加过（相同的数据库主机、端口只需启动一个CanalInstance）
        OracleInstance cachedInstance = CangoInstanceCache.getOracleInstance(key);
        if (cachedInstance == null) {
            try {
                String instancesInfo = cangoInstances.getHost() + ":" + cangoInstances.getPort() + "/" + cangoInstances.getDbName();
                LOGGER.info("start the OracleInstance ({}).", instancesInfo);
                if (StringUtils.isEmpty(cangoInstances.getDbName())) {
                    throw new YuGongException("schema should not be null!");
                }

                doStartYugong(cangoInstances, key, instancesInfo);
                LOGGER.info("start the OracleInstance ({}) successfully!", instancesInfo);
            } catch (Throwable e) {
                CangoInstanceCache.removeOracleInstance(key);
                LOGGER.error("Something goes wrong when starting up the OracleInstance:\n{}",
                        ExceptionUtils.getFullStackTrace(e));
            }
        } else {
            Set<String> filteredTables = filterTableList(cangoInstances.getTableNames());
            List<String> addedTables = cachedInstance.addTableName(new ArrayList<>(filteredTables));
            saveNewTables(addedTables, key);
        }
    }

    private void doStartYugong(CangoInstances cangoInstances, String key, String instancesInfo) {
        CangoInstances cangoInstancesPo = cangoInstancesService.findByName(cangoInstances);
        // 启动state为STOP的cangoInstance
        if (cangoInstancesPo != null) {
            if (State.STOP.getCode().equals(cangoInstancesPo.getState())) {
                CangoTable cangoTable = new CangoTable().setInstancesName(key);
                List<CangoTable> cangoTables = cangoTableNameService.findByInstancesName(cangoTable);
                OracleInstance instance = new OracleInstance(buildParams(cangoInstancesPo), cangoInstancesPo.getName());
                instance.addTableName(getStringTableName(cangoTables));
                doStartYugong0(cangoInstances, key, instance, instancesInfo);
            }
        } else {
            OracleInstance instance = new OracleInstance(buildParams(cangoInstances), cangoInstances.getName());
            doStartYugong0(cangoInstances, key, instance, instancesInfo);
        }
    }

    public void doStartYugong0(CangoInstances cangoInstances, String key, OracleInstance instance, String instancesInfo) {
        Set<String> filteredTables = filterTableList(cangoInstances.getTableNames());
        // 如果没有表，直接启动实例即可，且该实例的线程处于阻塞状态
        if (CollectionUtils.isEmpty(filteredTables)) {
            instance.start();
            CangoInstanceCache.addOracleInstance(key, instance);
            addShutDownHook(instance);
        } else {
            List<String> addedTables = instance.addTableName(new ArrayList<>(filteredTables));
            if (!CollectionUtils.isEmpty(addedTables)) {
                instance.start();
                CangoInstanceCache.addOracleInstance(key, instance);
                // 将成功添加到实例的表名持久化
                saveNewTables(addedTables, key);
                addShutDownHook(instance);
            } else {
                LOGGER.warn("OracleInstance ({}) execute addTableName method failed, tableNames {}", instancesInfo, String.valueOf(filteredTables));
            }
        }
    }

    /**
     * 持久化instance中的表
     *
     * @param newAddedTables the new added tables
     * @param key            the key
     */
    private void saveNewTables(Collection<String> newAddedTables, String key) {
        if (CollectionUtils.isEmpty(newAddedTables)) {
            List<CangoTable> cangoTables = new ArrayList<>();
            for (String tableName : newAddedTables) {
                CangoTable cangoTable = new CangoTable()
                        .setInstancesName(key)
                        .setTableName(tableName);
                cangoTables.add(cangoTable);
            }
            cangoTableNameService.batchSave(cangoTables);
        }
    }

    /**
     * 动态修改参数
     *
     * @param cangoInstances
     */
    private Map<OracleParamEnum, Object> buildParams(final CangoInstances cangoInstances) {
        Map<OracleParamEnum, Object> params = new EnumMap<OracleParamEnum, Object>(OracleParamEnum.class) {
            {
                put(OracleParamEnum.DB_USERNAME, cangoInstances.getUserName());
                put(OracleParamEnum.DB_PASSWORD, cangoInstances.getPassword());
                put(OracleParamEnum.DB_URL, "jdbc:oracle:thin:@" + cangoInstances.getHost() + ":" + cangoInstances.getPort() + ":" + cangoInstances.getDbName());
                put(OracleParamEnum.INSTANCE_NAME, getKey(cangoInstances));
                put(OracleParamEnum.DB_NAME, cangoInstances.getDbName());
                put(OracleParamEnum.DB_HOST, cangoInstances.getHost());
                put(OracleParamEnum.DB_PORT, cangoInstances.getPort());
            }
        };

        String blackTables = cangoInstances.getBlackTables();
        if (StringUtils.isNotBlank(blackTables)) {
            List<String> blackList = Arrays.asList(blackTables.split(","));
            params.put(OracleParamEnum.BLACK_TABLES, filterTableList(blackList));
        }
        return params;
    }

    private void addShutDownHook(final OracleInstance instance) {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                if (instance.isStart()) {
                    try {
                        LOGGER.info("stop the OracleInstance");
                        instance.stop();
                    } catch (Throwable e) {
                        LOGGER.warn("something goes wrong when stopping OracleInstance:\n{}",
                                ExceptionUtils.getFullStackTrace(e));
                    } finally {
                        LOGGER.info("OracleInstance is down.");
                    }
                }
            }

        });
    }

    @Override
    public CangoResponseDto start(CangoRequestDto cangoRequestDto) {
        return internalStart(cangoRequestDto, State.STOP);
    }

    @Override
    public CangoResponseDto enable(CangoRequestDto cangoRequestDto) {
        return internalStart(cangoRequestDto, State.DISABLE);
    }

    /**
     * 启动实例.
     *
     * @param cangoRequestDto the canal req dto
     * @param startState      启动匹配类型的实例
     * @return the canal rsp dto
     * @date : 2016-07-05 13:58:14
     */
    private synchronized CangoResponseDto internalStart(CangoRequestDto cangoRequestDto, State startState) {
        CangoResponseDto cangoResponseDto = new CangoResponseDto();

        //创建对象
        CangoInstances cangoInstances = new CangoInstances();
        BeanUtils.copyProperties(cangoRequestDto, cangoInstances);

        //检查是否已经添加过
        CangoInstances cangoInstancesPo = cangoInstancesService.findByName(cangoInstances);
        if (cangoInstancesPo != null) {//已添加过
            //当前处于不可用状态
            if (startState.getCode().equals(cangoInstancesPo.getState())) {
                CangoTable cangoTable = new CangoTable();
                cangoTable.setInstancesName(CangoInstanceCache.createKey(cangoInstancesPo.getHost(), cangoInstancesPo.getPort()));
                List<CangoTable> pTableNames = cangoTableNameService.findByInstancesName(cangoTable);
                if (DbType.ORACLE.getCode().equals(cangoInstancesPo.getDbType())) {
                    if (!CollectionUtils.isEmpty(pTableNames)) {
                        List<String> tableNames = new ArrayList<>(pTableNames.size());
                        for (CangoTable pTableName : pTableNames) {
                            tableNames.add(pTableName.getTableName());
                        }
                        cangoInstancesPo.setTableNames(tableNames);
                    }
                    startYuGong(cangoInstancesPo);
                } else {
                    startCanal(cangoInstancesPo);
                }

                cangoInstancesPo.setState(State.START.getCode());
                cangoInstancesPo.setUpdateTime(new Date());
                cangoInstancesService.updateState(cangoInstancesPo);
            }

            //成功响应
            cangoResponseDto.setStatus(CangoRspStatus.SUCCESS);
            return cangoResponseDto;
        } else { //未添加过
            cangoResponseDto.setStatus(CangoRspStatus.FAILURE);
            cangoResponseDto.setFailMsg(String.format("cannot find CangoInstance(%s)!", cangoRequestDto.getName()));
        }

        return cangoResponseDto;
    }

    @Override
    public CangoResponseDto stop(CangoRequestDto canalReqDto) {
        return internalStop(canalReqDto, State.STOP);
    }

    @Override
    public CangoResponseDto disable(CangoRequestDto canalReqDto) {
        return internalStop(canalReqDto, State.DISABLE);
    }

    /**
     * 停用实例，并持久化实例状态.
     *
     * @param canalReqDto the canal req dto
     * @param stopState   需要持久化的状态
     * @return the canal rsp dto
     * @date : 2016-07-05 13:50:06
     */
    private synchronized CangoResponseDto internalStop(CangoRequestDto canalReqDto, State stopState) {
        CangoResponseDto CangoResultDto = new CangoResponseDto();

        //创建对象
        CangoInstances cangoInstances = new CangoInstances();
        BeanUtils.copyProperties(canalReqDto, cangoInstances);

        //检查是否已经添加过
        CangoInstances cangoInstancesPo = cangoInstancesService.findByName(cangoInstances);
        if (cangoInstancesPo != null) {//已添加过
            //当前处于可用状态
            if (State.START.getCode().equals(cangoInstancesPo.getState())) {
                cangoInstancesPo.setState(stopState.getCode());
                cangoInstancesPo.setUpdateTime(new Date());
                cangoInstancesService.updateState(cangoInstancesPo);

                if (DbType.ORACLE.getCode().equals(cangoInstancesPo.getDbType())) {
                    stopYuGong(cangoInstancesPo);
                } else {
                    stopCanal(cangoInstancesPo, false);
                }
            }

            //成功响应
            CangoResultDto.setStatus(CangoRspStatus.SUCCESS);
            return CangoResultDto;
        } else { //未添加过
            CangoResultDto.setStatus(CangoRspStatus.FAILURE);
            CangoResultDto.setFailMsg(String.format("cannot find CangoInstance(%s)!", canalReqDto.getName()));
        }
        return CangoResultDto;
    }

    /**
     * Stop canal.
     *
     * @param cangoInstances the canal instances
     * @param force          是否强制停用canal实例
     * @date :2016-04-25 16:04:01
     */
    private int stopCanal(CangoInstances cangoInstances, boolean force) {
        int countMysqlInstance = 0;
        //查看该数据库实例是否已启用
        String key = getKey(cangoInstances);
        //该数据库实例是否已经添加过（相同的数据库主机、端口只需启动一个CanalInstance）
        MysqlInstance mysqlInstance = CangoInstanceCache.getMysqlInstance(key);
        if (mysqlInstance != null) {
            if (force) {
                CangoInstances condition = new CangoInstances()
                        .setHost(cangoInstances.getHost())
                        .setPort(cangoInstances.getPort())
                        .setState(State.START.getCode())
                        .setDbType(DbType.MYSQL.getCode());

                // 在调用stopCanal之前，该canal实例已经持久化过
                List<CangoInstances> cangoInstancesList = cangoInstancesService.findByCondition(condition);
                if (!CollectionUtils.isEmpty(cangoInstancesList)) {
                    int stopState = cangoInstances.getState();
                    Date updateTime = new Date();

                    for (CangoInstances instances : cangoInstancesList) {
                        // 持久化
                        instances.setState(stopState);
                        instances.setUpdateTime(updateTime);
                        cangoInstancesService.updateState(instances);
                        // stop
                        mysqlInstance.stop();
                        countMysqlInstance++;
                    }
                }
            }

            mysqlInstance.stop();
            countMysqlInstance++;

            if (!mysqlInstance.isStart()) {
                CangoInstanceCache.removeMysqlInstance(key);
            }
        }

        return countMysqlInstance;
    }

    /**
     * Stop yu gong.
     *
     * @param cangoInstances the canal instances
     * @date : 2016-07-05 14:07:19
     */
    private void stopYuGong(CangoInstances cangoInstances) {
        String key = getKey(cangoInstances);
        LOGGER.info("closing OracleInstance, name = {}, host = {}, dbName = {}", cangoInstances.getName(), cangoInstances.getHost(), cangoInstances.getDbName());
        OracleInstance instance = CangoInstanceCache.getOracleInstance(key);
        if (instance != null) {
            instance.stop();
            if (instance.isStop()) {
                CangoInstanceCache.removeOracleInstance(key);
            }
        } else {
            LOGGER.warn("OracleInstance close failed");
        }
    }

    @Override
    public CangoResponseDto check(CangoRequestDto canalReqDto) {
        CangoResponseDto CangoResultDto = new CangoResponseDto();
        CangoInstances cangoInstances = new CangoInstances();
        BeanUtils.copyProperties(canalReqDto, cangoInstances);
        //检查是否已经添加过
        CangoInstances cangoInstancesPo = cangoInstancesService.findByName(cangoInstances);
        if (cangoInstancesPo != null) { // 已添加过
            CangoResultDto.setStatus(CangoRspStatus.SUCCESS);
            CangoResultDto.setMsg(JSON.toJSONString(cangoInstancesPo));
            return CangoResultDto;
        } else {                        // 未添加过
            CangoResultDto.setStatus(CangoRspStatus.FAILURE);
            CangoResultDto.setFailMsg(String.format("cannot find CangoInstance(%s)!", canalReqDto.getName()));
            return CangoResultDto;
        }
    }

    @Override
    public synchronized CangoResponseDto startAll() {
        LOGGER.info("start all cangoInstance whose state is STOP!");
        CangoInstances condition = new CangoInstances();
        condition.setState(State.STOP.getCode());
        // 将STOP的全部实例启动！
        List<CangoInstances> instancesList = cangoInstancesService.findByCondition(condition);
        int countMysqlInstance = 0;
        int countOracleInstance = 0;
        if (!CollectionUtils.isEmpty(instancesList)) {
            Map<String, Integer> countMap = internalStartCanalAndYuGong(instancesList, null);
            countMysqlInstance = countMap.get(CANAL_INSTANCE);
            countOracleInstance = countMap.get(ORACLE_INSTANCE);

            // 批量更新
            cangoInstancesService.batchUpdateState(instancesList);
        }
        LOGGER.info("start all cangoInstance successfully whose state is STOP!，count MysqlInstance number {}, OracleInstance number {}!",
                countMysqlInstance, countOracleInstance);
        return new CangoResponseDto().setStatus(CangoRspStatus.SUCCESS);
    }

    @Override
    public synchronized CangoResponseDto shutdown() {
        stopAll();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().sleep(5000);
                    LOGGER.info("stop and exit cango successfully!");
                    System.exit(0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        return new CangoResponseDto().setStatus(CangoRspStatus.SUCCESS);
    }

    @Override
    public synchronized CangoResponseDto stopAll() {
        Map<String, MysqlInstance> mysqlInstanceMap = CangoInstanceCache.getMysqlInstanceMap();
        Map<String, OracleInstance> oracleInstanceMap = CangoInstanceCache.getOracleInstanceMap();

        if (!CollectionUtils.isEmpty(mysqlInstanceMap) || !CollectionUtils.isEmpty(oracleInstanceMap)) {
            try {
                int canalInstanceSize = 0;
                int oracleInstanceSize = 0;
                if (!CollectionUtils.isEmpty(mysqlInstanceMap)) {
                    for (MysqlInstance mysqlInstance : mysqlInstanceMap.values()) {
                        CangoInstances cangoInstances = new CangoInstances();
                        cangoInstances.setName(mysqlInstance.getName());
                        CangoInstances cangoInstancesPo = cangoInstancesService.findByName(cangoInstances);
                        if (cangoInstancesPo != null) {
                            if (State.START.getCode().equals(cangoInstancesPo.getState())) {
                                cangoInstancesPo.setState(State.STOP.getCode());
                                cangoInstancesPo.setUpdateTime(new Date());
                                cangoInstancesService.updateState(cangoInstancesPo);
                                int count = stopCanal(cangoInstancesPo, true);
                                canalInstanceSize += count;
                            }
                        }
                    }

                    mysqlInstanceMap.clear();
                }

                if (!CollectionUtils.isEmpty(oracleInstanceMap)) {
                    for (OracleInstance oracleInstance : oracleInstanceMap.values()) {
                        CangoInstances cangoInstances = new CangoInstances();
                        cangoInstances.setName(oracleInstance.getName());
                        CangoInstances cangoInstancesPo = cangoInstancesService.findByName(cangoInstances);
                        if (cangoInstancesPo != null) {
                            //当前处于可用状态
                            if (State.START.getCode().equals(cangoInstancesPo.getState())) {
                                cangoInstancesPo.setState(State.STOP.getCode());
                                cangoInstancesPo.setUpdateTime(new Date());
                                cangoInstancesService.updateState(cangoInstancesPo);
                                stopYuGong(cangoInstancesPo);
                                oracleInstanceSize++;
                            }
                        }
                    }

                    oracleInstanceMap.clear();
                }
                LOGGER.info("stopAll cangoInstance, count closed MysqlInstance number{}, OracleInstance number{}", canalInstanceSize, oracleInstanceSize);
            } catch (Exception e) {
                LOGGER.error("stopAll cangoInstance has an exception!", ExceptionUtils.getFullStackTrace(e.getCause()));
            }
        }
        return new CangoResponseDto().setStatus(CangoRspStatus.SUCCESS);
    }

    @Override
    public synchronized CangoResponseDto clearAll() {
        stopAll();
        clearAllInstance();
        LOGGER.info("clear all cangoInstance successfully");
        return new CangoResponseDto().setStatus(CangoRspStatus.SUCCESS);
    }

    /**
     * 清空canal_instances表和canal_tablenames表.
     *
     * @date : 2016-07-19 14:12:18
     */
    private void clearAllInstance() {
        cangoInstancesService.deleteAll();
        cangoTableNameService.deleteAll();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        // 容器启动完成后装载之前已添加的实例
        if (applicationEvent instanceof ContextRefreshedEvent) {
            LOGGER.info("spring container start all persisted cangoInstance!");
            CangoInstances condition = new CangoInstances();
            condition.setState(State.START.getCode());
            List<CangoInstances> instancesList = cangoInstancesService.findByCondition(condition);
            int countMysqlInstance = 0;
            int countOracleInstance = 0;
            if (!CollectionUtils.isEmpty(instancesList)) {
                Map<String, Integer> countMap = internalStartCanalAndYuGong(instancesList, null);
                countMysqlInstance = countMap.get(CANAL_INSTANCE);
                countOracleInstance = countMap.get(ORACLE_INSTANCE);
            }

            LOGGER.info("spring container start all persisted cangoInstance successfully, count MysqlInstance number {}, OracleInstance number {}!",
                    countMysqlInstance, countOracleInstance);
        }

        if (applicationEvent instanceof ContextClosedEvent) {
            shutdown();
        }
    }

    /**
     * Get key string.
     *
     * @param cangoInstances the canal instances
     * @return the string
     */
    private String getKey(CangoInstances cangoInstances) {
        return CangoInstanceCache.createKey(cangoInstances.getHost(), cangoInstances.getPort());
    }

    /**
     * 启动list中所有的canal和yugong.
     *
     * @param instancesList the instances list
     * @param logParseTime  the logParseTime
     * @return the map
     * @date : 2016-07-05 16:39:37
     */
    private Map<String, Integer> internalStartCanalAndYuGong(List<CangoInstances> instancesList, Date logParseTime) {
        // canal实例数目
        int countMysqlInstance = 0;
        // yugong实例数目
        int countOracleInstance = 0;
        Map<String, Integer> countMap = new HashMap<>();

        for (CangoInstances cangoInstances : instancesList) {
            //查看该数据库实例是否已启用
            if (DbType.ORACLE.getCode().equals(cangoInstances.getDbType())) {
                startYuGong(cangoInstances);
                countOracleInstance++;
            } else {
                startCanal(cangoInstances);
                countMysqlInstance++;
            }

            // 重置为启用状态
            cangoInstances.setState(State.START.getCode());
        }

        countMap.put(CANAL_INSTANCE, countMysqlInstance);
        countMap.put(ORACLE_INSTANCE, countOracleInstance);
        return countMap;
    }
}
