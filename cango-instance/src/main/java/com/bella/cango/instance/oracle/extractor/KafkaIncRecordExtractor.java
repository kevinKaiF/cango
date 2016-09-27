package com.bella.cango.instance.oracle.extractor;

import com.bella.cango.exception.CangoException;
import com.bella.cango.instance.oracle.common.db.meta.ColumnMeta;
import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.bella.cango.instance.oracle.common.db.meta.Table;
import com.bella.cango.instance.oracle.common.db.meta.TableMetaGenerator;
import com.bella.cango.instance.oracle.common.db.sql.SqlTemplates;
import com.bella.cango.instance.oracle.common.model.ExtractStatus;
import com.bella.cango.instance.oracle.common.model.YuGongContext;
import com.bella.cango.instance.oracle.common.model.record.IncrementOpType;
import com.bella.cango.instance.oracle.common.model.record.KafkaIncrementRecord;
import com.bella.cango.instance.oracle.exception.YuGongException;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.CollectionUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.*;

public class KafkaIncRecordExtractor extends AbstractOracleRecordExtractor {
    private static final String MLOG_EXTRACT_FORMAT = "select rowid,{0} from {1}.{2} where rownum <= ? order by SEQUENCE$$ asc";
    private static final String MLOG_CLEAN_FORMAT = "delete from {0}.{1} where rowid=?";
    private static final String SINGLE_EXTRACT_FORMAT = "select rowid,{0} from {1}.{2} where SEQUENCE$$ = ?";
    private static final String CREATE_MLOG_FORMAT = "CREATE MATERIALIZED VIEW LOG ON {0}.{1} WITH SEQUENCE({2}), {3} INCLUDING NEW VALUES ";
    private YuGongContext context;
    private ColumnMeta rowidColumn = new ColumnMeta("rowid", Types.ROWID);
    private long sleepTime = 1000L;

    public KafkaIncRecordExtractor(YuGongContext context) {
        this.context = context;
    }

    public void start() {
        super.start();
    }

    public void stop() {
        super.stop();
    }

    @Override
    public Map<String, Object> extract(Table table) throws YuGongException {
        Map<String, Object> map = initParams(table);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getSourceDs());
        Map<String, Object> rowIdsAndRecords = getMlogRecord(jdbcTemplate, map, table);
        List<KafkaIncrementRecord> kafkaIncrementRecords = (List<KafkaIncrementRecord>) rowIdsAndRecords.get("kafkaIncrementRecords");
        if (kafkaIncrementRecords.size() == 0) {
            setStatus(ExtractStatus.NO_UPDATE);
            logger.info("table[{}] now is {} ...", table.getFullName(), status);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();// 传递下去
                return new HashMap<>();
            }
        } else if (kafkaIncrementRecords.size() < context.getOnceCrawNum()) {
            setStatus(ExtractStatus.CATCH_UP);
            logger.info("table[{}] now is {} ...", table.getFullName(), status);
        }

        return rowIdsAndRecords;
    }

    private Map<String, Object> initParams(Table table) {
        Map<String, Object> map = new HashMap<>();
        String schemaName = table.getSchema();
        String tableName = table.getName();

        // 必须指定schema，因为不同表空间下，有可能表名是相同的
        if (StringUtils.isEmpty(schemaName)) {
            throw new YuGongException("schema should not be null! please set schema for table [" + tableName + "]");
        }

        // 后去mlog表名
        String mlogTableName = TableMetaGenerator.getMLogTableName(context.getSourceDs(), schemaName, tableName);
        if (StringUtils.isEmpty(mlogTableName)) {
            // 如果没有就创建
            markIncPosition(table);
            mlogTableName = TableMetaGenerator.getMLogTableName(context.getSourceDs(), schemaName, tableName);
        }

        if (StringUtils.isEmpty(mlogTableName)) {
            throw new YuGongException("not found mlog table for [" + schemaName + "." + tableName + "]");
        }

        // 获取mlog表结构
        Table mlogMeta = TableMetaGenerator.getTableMeta(context.getSourceDs(),
                table.getSchema(),
                mlogTableName);


        // 构造mlog sql
        String colstr = SqlTemplates.COMMON.makeColumn(mlogMeta.getColumns());
        String mlogExtractSql = new MessageFormat(MLOG_EXTRACT_FORMAT).format(new Object[]{colstr, schemaName, mlogTableName});
        String singleMlogExtractSql = new MessageFormat(SINGLE_EXTRACT_FORMAT).format(new Object[]{colstr, schemaName, mlogTableName});
        String mlogCleanSql = new MessageFormat(MLOG_CLEAN_FORMAT).format(new Object[]{schemaName, mlogTableName});

        map.put("mlogMeta", mlogMeta);
        map.put("mlogExtractSql", mlogExtractSql);
        map.put("singleMlogExtractSql", singleMlogExtractSql);
        map.put("mlogCleanSql", mlogCleanSql);
        return map;
    }

    private void markIncPosition(Table table) {
        String schemaName = table.getSchema();
        String tableName = table.getName();
        String columnStr = SqlTemplates.COMMON.makeColumn(table.getColumns());
        String createMlogSql = MessageFormat.format(CREATE_MLOG_FORMAT, new Object[]{schemaName, tableName, columnStr,
                " primary key"});

        String mlogName = TableMetaGenerator.getMLogTableName(context.getSourceDs(), schemaName, tableName);
        if (mlogName == null) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getSourceDs());
            String url = null;
            try {
                url = context.getSourceDs().getConnection().getMetaData().getURL();
                jdbcTemplate.execute(createMlogSql);
            } catch (Exception e) {
                throw new CangoException("oracle connect exception，URL:" + url + ", caused :" + e.getMessage(), e);
            }
            // 基于MLOG不需要返回position
            logger.info("create mlog successed. sql : {}", createMlogSql);
        } else {
            logger.warn("mlog[{}] is exist, just have fun. ", mlogName);
        }
    }

    @Override
    public void clearMlog(final List<ColumnValue> records, String mlogCleanSql) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getSourceDs());
        jdbcTemplate.execute(mlogCleanSql, new PreparedStatementCallback() {

            @Override
            public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                for (ColumnValue record : records) {
                    ps.setObject(1, record.getValue(), record.getColumn().getType());
                    ps.addBatch();
                }
                ps.executeBatch();
                return null;
            }
        });
    }

    private Map<String, Object> getMlogRecord(JdbcTemplate jdbcTemplate, Map<String, Object> map, final Table table) {
        final List<ColumnValue> rowIds = new ArrayList<ColumnValue>();
        String mlogExtractSql = (String) map.get("mlogExtractSql");
        final Table tableMeta = (Table) map.get("mlogMeta");
        final String singleMlogExtractSql = (String) map.get("singleMlogExtractSql");
        List<KafkaIncrementRecord> kafkaIncrementRecords = (List<KafkaIncrementRecord>) jdbcTemplate.execute(mlogExtractSql, new PreparedStatementCallback() {

            public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                ps.setInt(1, context.getOnceCrawNum());

                List<KafkaIncrementRecord> result = Lists.newArrayList();
                ResultSet rs = ps.executeQuery();
                try {
                    while (rs.next()) {

                        IncrementOpType opType = getDmlType(rs);
                        KafkaIncrementRecord record = null;
                        switch (opType) {
                            case I:
                                record = buildInsertRecord(rs, context, tableMeta.getColumns(), table, null, rowIds);
                                break;
                            case U:
                                record = buildUpdateRecord(rs, context, tableMeta.getColumns(), table, singleMlogExtractSql, rowIds);
                                break;
                            case D:
                                record = buildDeleteRecord(rs, context, tableMeta.getColumns(), table, null, rowIds);
                                break;
                        }

                        if (record != null) {
                            result.add(record);
                        }
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }

                return result;
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("rowIds", rowIds);
        result.put("kafkaIncrementRecords", kafkaIncrementRecords);
        result.put("mlogCleanSql", map.get("mlogCleanSql"));
        return result;
    }

    private KafkaIncrementRecord buildDeleteRecord(ResultSet rs, YuGongContext context, List<ColumnMeta> mlogCols, Table table, String sql, List<ColumnValue> rowIds) throws SQLException {
        List<ColumnValue> columns = new ArrayList<ColumnValue>();
        List<ColumnValue> primaryKeys = buildColumnValue(rs, context.getSourceEncoding(), mlogCols, table.getPrimaryKeys());
        List<ColumnValue> beforeColumns = buildColumnValue(rs, context.getSourceEncoding(), mlogCols, table.getColumns());

        ColumnValue rowId = new ColumnValue(rowidColumn, rs.getObject("rowid"));
        KafkaIncrementRecord record = new KafkaIncrementRecord(table.getSchema(),
                table.getName(),
                primaryKeys,
                columns);

        rowIds.add(rowId);
        record.setRowId(rowId);
        record.setExecuteTime(System.currentTimeMillis());
        record.setOpType(IncrementOpType.D);
        record.setBeforeColumns(beforeColumns);
        record.setAfterColumns(null);
        return record;
    }


    private KafkaIncrementRecord buildUpdateRecord(ResultSet rs, YuGongContext context, List<ColumnMeta> mlogCols, Table table, String singleMlogExtractSql, List<ColumnValue> rowIds) throws SQLException {
        ColumnValue rowId = new ColumnValue(rowidColumn, rs.getObject("rowid"));
        rowIds.add(rowId);

        // 数据更改标志位，O或U表示变更前的老数据，N表示变更后的新数据
        String oldOrNew = rs.getString("OLD_NEW$$");
        if (StringUtils.equals("N", oldOrNew)) {
            List<ColumnValue> columns = new LinkedList<ColumnValue>();
            List<ColumnValue> primaryKeys = buildColumnValue(rs, context.getSourceEncoding(), mlogCols, table.getPrimaryKeys());
            List<ColumnValue> afterColumns = buildColumnValue(rs, context.getSourceEncoding(), mlogCols, table.getColumns());
            List<ColumnValue> beforeColumns = getPrevUpdateRecord(singleMlogExtractSql, rs, context, mlogCols, table.getColumns(), rowIds);

            if (CollectionUtils.isEmpty(beforeColumns)) {
                logger.warn("the OLD_NEW$$ = 'N' of MLOG$_{} without sibling causes that will be ignored ", table.getFullName());
                return null;
            } else {
                KafkaIncrementRecord record = new KafkaIncrementRecord(table.getSchema(),
                        table.getName(),
                        primaryKeys,
                        columns);
                record.setRowId(rowId);
                record.setExecuteTime(System.currentTimeMillis());
                record.setOpType(IncrementOpType.U);
                record.setBeforeColumns(beforeColumns);
                record.setAfterColumns(afterColumns);
                return record;
            }
        } else {  // O或者U
            List<ColumnValue> columns = new LinkedList<ColumnValue>();
            List<ColumnValue> primaryKeys = buildColumnValue(rs, context.getSourceEncoding(), mlogCols, table.getPrimaryKeys());
            List<ColumnValue> beforeColumns = buildColumnValue(rs, context.getSourceEncoding(), mlogCols, table.getColumns());
            List<ColumnValue> afterColumns = getNextUpdateRecord(singleMlogExtractSql, rs, context, mlogCols, table.getColumns(), rowIds);

            if (CollectionUtils.isEmpty(afterColumns)) {
                logger.warn("the OLD_NEW$$ = 'O' of MLOG$_{} without sibling causes that will be ignored ", table.getFullName());
                return null;
            } else {
                KafkaIncrementRecord record = new KafkaIncrementRecord(table.getSchema(),
                        table.getName(),
                        primaryKeys,
                        columns);
                record.setRowId(rowId);
                record.setExecuteTime(System.currentTimeMillis());
                record.setOpType(IncrementOpType.U);
                record.setBeforeColumns(beforeColumns);
                record.setAfterColumns(afterColumns);
                return record;
            }
        }
    }

    /**
     * 获取UPDATE中上一条mlog日志
     *
     * @param rs
     * @param context
     * @param rowIds
     * @return
     */
    private List<ColumnValue> getPrevUpdateRecord(String singleMlogExtractSql, ResultSet rs, YuGongContext context, List<ColumnMeta> mlogCols, List<ColumnMeta> columns, List<ColumnValue> rowIds) throws SQLException {
        int sequence = rs.getInt("SEQUENCE$$");
        return getSingleMlogRecord(singleMlogExtractSql, sequence - 1, context, mlogCols, columns, rowIds);
    }

    /**
     * 获取UPDATE中下一条mlog日志
     *
     * @param rs
     * @param context
     * @param rowIds
     * @return
     */
    private List<ColumnValue> getNextUpdateRecord(String singleMlogExtractSql, ResultSet rs, YuGongContext context, List<ColumnMeta> columns, List<ColumnMeta> mlogCols, List<ColumnValue> rowIds) throws SQLException {
        ResultSet curr = rs;
        int sequence = curr.getInt("SEQUENCE$$");
        if (curr.next() && curr.getInt("SEQUENCE$$") == (sequence + 1)) {
            rowIds.add(new ColumnValue(rowidColumn, curr.getObject("rowid")));
            return buildColumnValue(curr, context.getSourceEncoding(), mlogCols, columns);
        } else {
            return getSingleMlogRecord(singleMlogExtractSql, sequence + 1, context, mlogCols, columns, rowIds);
        }
    }

    /**
     * 根据mlog物化日志表的SEQUENCE$$字段单独查询
     *
     * @param sequence
     * @param context
     * @param columns
     * @return
     */
    private List<ColumnValue> getSingleMlogRecord(final String singleMlogExtractSql, final int sequence, final YuGongContext context, final List<ColumnMeta> mlogCols, final List<ColumnMeta> columns, final List<ColumnValue> rowIds) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getSourceDs());
        final List<ColumnValue> columnValues = new ArrayList<ColumnValue>();
        return (List<ColumnValue>) jdbcTemplate.execute(singleMlogExtractSql, new PreparedStatementCallback() {
            @Override
            public List<ColumnValue> doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                ps.setInt(1, sequence);
                ResultSet rs = ps.executeQuery();
                try {
                    while (rs.next()) {
                        rowIds.add(new ColumnValue(rowidColumn, rs.getObject("rowid")));
                        columnValues.addAll(buildColumnValue(rs, context.getSourceEncoding(), mlogCols, columns));
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }
                return columnValues;
            }
        });
    }

    private KafkaIncrementRecord buildInsertRecord(ResultSet rs, YuGongContext context, List<ColumnMeta> mlogCols, Table table, String sql, List<ColumnValue> rowIds) throws SQLException {
        List<ColumnValue> columns = new LinkedList<ColumnValue>();
        List<ColumnValue> primaryKeys = buildColumnValue(rs, context.getSourceEncoding(), mlogCols, table.getPrimaryKeys());
        List<ColumnValue> afterColumns = buildColumnValue(rs, context.getSourceEncoding(), mlogCols, table.getColumns());

        ColumnValue rowId = new ColumnValue(rowidColumn, rs.getObject("rowid"));
        rowIds.add(rowId);

        KafkaIncrementRecord record = new KafkaIncrementRecord(table.getSchema(),
                table.getName(),
                primaryKeys,
                columns);

        record.setRowId(rowId);
        record.setExecuteTime(System.currentTimeMillis());
        record.setOpType(IncrementOpType.I);
        record.setBeforeColumns(null);
        record.setAfterColumns(afterColumns);
        return record;
    }

    /**
     * 将mlog中数据记录与context中的table列对应并解析
     *
     * @param rs              结果集
     * @param encoding        数据编码格式
     * @param mColumnMeta     mlog表的元数据
     * @param tableColumnMeta 当前表的元数据
     * @return
     * @throws java.sql.SQLException
     */
    private List<ColumnValue> buildColumnValue(ResultSet rs, String encoding, List<ColumnMeta> mColumnMeta, List<ColumnMeta> tableColumnMeta) throws SQLException {
        List<ColumnValue> columnValues = new LinkedList<ColumnValue>();
        for (ColumnMeta column : tableColumnMeta) {
            if (contains(mColumnMeta, column.getName())) {
                ColumnValue columnValue = getColumnValue(rs, encoding, column);
                columnValues.add(columnValue);
            }
        }
        return columnValues;
    }


    private boolean contains(List<ColumnMeta> columnMetaList, String columnName) {
        for (ColumnMeta columnMeta : columnMetaList) {
            if (columnMeta.getName().equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    private IncrementOpType getDmlType(ResultSet rs) throws SQLException {
        String dmlType = rs.getString("DMLTYPE$$");
        return IncrementOpType.valueOf(dmlType);
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

}
