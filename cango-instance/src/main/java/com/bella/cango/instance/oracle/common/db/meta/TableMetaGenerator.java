package com.bella.cango.instance.oracle.common.db.meta;

import com.bella.cango.instance.oracle.common.utils.LikeUtil;
import com.bella.cango.instance.oracle.exception.YuGongException;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 基于mysql的table meta获取
 *
 * @author agapple 2013-9-9 下午2:45:30
 * @since 3.0.0
 */
public class TableMetaGenerator {

    private static final String mlogQuerySql = "select master,log_table from all_mview_logs where master = ?";
    private static final String mlogSchemaQuerySql = "select master,log_table from all_mview_logs where master = ? and log_owner = ?";
    private static final String queryShardKey = "show partitions from ?";

    /**
     * 获取对应的table meta信息，精确匹配
     *
     * @param dataSource
     * @param schemaName
     * @param tableName
     * @return
     */
    public static Table getTableMeta(final DataSource dataSource, final String schemaName, final String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return (Table) jdbcTemplate.execute(new ConnectionCallback() {

            public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
                DatabaseMetaData metaData = conn.getMetaData();
                String sName = getIdentifierName(schemaName, metaData);
                String tName = getIdentifierName(tableName, metaData);

                ResultSet rs = null;
                Table table;
                try {
                    rs = metaData.getTables(sName, sName, tName, new String[]{"TABLE"});
                    table = null;
                    while (rs.next()) {
                        String catlog = rs.getString(1);
                        String schema = rs.getString(2);
                        String name = rs.getString(3);
                        String type = rs.getString(4);

                        if ((sName == null || LikeUtil.isMatch(sName, catlog) || LikeUtil.isMatch(sName, schema))
                                && LikeUtil.isMatch(tName, name)) {
                            table = new Table(type, StringUtils.isEmpty(catlog) ? schema : catlog, name);
                            break;
                        }
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }

                if (table == null) {
                    throw new YuGongException("table[" + schemaName + "." + tableName + "] is not found");
                }

                // 查询所有字段
                List<ColumnMeta> columnList;
                try {
                    rs = metaData.getColumns(sName, sName, tName, null);
                    columnList = new ArrayList<ColumnMeta>();
                    while (rs.next()) {
                        String catlog = rs.getString(1);
                        String schema = rs.getString(2);
                        String name = rs.getString(3);
                        if ((sName == null || LikeUtil.isMatch(sName, catlog) || LikeUtil.isMatch(sName, schema))
                                && LikeUtil.isMatch(tName, name)) {
                            String columnName = rs.getString(4); // COLUMN_NAME
                            int columnType = rs.getInt(5);
                            String typeName = rs.getString(6);
                            columnType = convertSqlType(columnType, typeName);
                            ColumnMeta col = new ColumnMeta(columnName, columnType);
                            columnList.add(col);
                        }
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }

                // 查询主键信息
                List<String> primaryKeys = new ArrayList<String>();
                try {
                    rs = metaData.getPrimaryKeys(sName, sName, tName);
                    while (rs.next()) {
                        String catlog = rs.getString(1);
                        String schema = rs.getString(2);
                        String name = rs.getString(3);
                        if ((sName == null || LikeUtil.isMatch(sName, catlog) || LikeUtil.isMatch(sName, schema))
                                && LikeUtil.isMatch(tName, name)) {
                            primaryKeys.add(StringUtils.upperCase(rs.getString(4)));
                        }
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }

                List<String> uniqueKeys = new ArrayList<String>();
                if (primaryKeys.isEmpty()) {
                    String lastIndexName = null;
                    try {
                        rs = metaData.getIndexInfo(sName, sName, tName, true, true);
                        while (rs.next()) {
                            String catlog = rs.getString(1);
                            String schema = rs.getString(2);
                            String name = rs.getString(3);
                            if ((sName == null || LikeUtil.isMatch(sName, catlog) || LikeUtil.isMatch(sName, schema))
                                    && LikeUtil.isMatch(tName, name)) {
                                String indexName = StringUtils.upperCase(rs.getString(6));
                                if ("PRIMARY".equals(indexName)) {
                                    continue;
                                }

                                if (lastIndexName == null) {
                                    lastIndexName = indexName;
                                } else if (!lastIndexName.equals(indexName)) {
                                    break;
                                }

                                uniqueKeys.add(StringUtils.upperCase(rs.getString(9)));
                            }
                        }
                    } finally {
                        JdbcUtils.closeResultSet(rs);
                    }

                    // 如果无主键，使用唯一键
                    primaryKeys.addAll(uniqueKeys);
                }

                Set<ColumnMeta> columns = new HashSet<ColumnMeta>();
                Set<ColumnMeta> pks = new HashSet<ColumnMeta>();
                for (ColumnMeta columnMeta : columnList) {
                    if (primaryKeys.contains(columnMeta.getName())) {
                        pks.add(columnMeta);
                    } else {
                        columns.add(columnMeta);
                    }
                }

                table.getColumns().addAll(columns);
                table.getPrimaryKeys().addAll(pks);
                return table;
            }

        });
    }

    /**
     * 查询所有的表，不返回表中的字段
     *
     * @param dataSource
     * @return
     */
    public static List<Table> getTableMetasWithoutColumn(final DataSource dataSource, final String schemaName,
                                                         final String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return (List<Table>) jdbcTemplate.execute(new ConnectionCallback() {

            public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
                DatabaseMetaData metaData = conn.getMetaData();
                List<Table> result = Lists.newArrayList();
                String databaseName = metaData.getDatabaseProductName();
                String sName = getIdentifierName(schemaName, metaData);
                String tName = getIdentifierName(tableName, metaData);
                ResultSet rs = null;
                Table table = null;
                if (StringUtils.startsWithIgnoreCase(databaseName, "oracle") && StringUtils.isEmpty(schemaName)
                        && StringUtils.isEmpty(tableName)) {
                    // 针对oracle，只查询用户表，忽略系统表
                    Statement stmt = null;
                    try {
                        stmt = conn.createStatement();
                        rs = stmt.executeQuery("SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS SCHEMA_NAME , TABLE_NAME FROM USER_TABLES T , USER_USERS U WHERE U.USERNAME = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')");

                        while (rs.next()) {
                            String schema = rs.getString(1);
                            String name = rs.getString(2);
                            if (!StringUtils.startsWithIgnoreCase(name, "MLOG$_")) {
                                table = new Table("TABLE", schema, name);
                                result.add(table);
                            }
                        }
                    } finally {
                        JdbcUtils.closeResultSet(rs);
                        JdbcUtils.closeStatement(stmt);
                    }

                    return result;
                } else {
                    try {
                        rs = metaData.getTables(sName, sName, tName, new String[]{"TABLE"});
                        while (rs.next()) {
                            String catlog = rs.getString(1);
                            String schema = rs.getString(2);
                            String name = rs.getString(3);
                            String type = rs.getString(4);

                            if (!StringUtils.startsWithIgnoreCase(name, "MLOG$_")) {
                                table = new Table(type, StringUtils.isEmpty(catlog) ? schema : catlog, name);
                                result.add(table);
                            }
                        }
                    } finally {
                        JdbcUtils.closeResultSet(rs);
                    }
                    return result;
                }
            }

        });
    }

    /**
     * 返回就诶过 key:column name , value=index name
     */
    public static Map<String/* column name */, String /* index name */> getTableIndex(final DataSource dataSource,
                                                                                      final String schemaName,
                                                                                      final String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return (Map<String, String>) jdbcTemplate.execute(new ConnectionCallback() {

            public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
                DatabaseMetaData metaData = conn.getMetaData();
                String sName = getIdentifierName(schemaName, metaData);
                String tName = getIdentifierName(tableName, metaData);

                ResultSet rs = null;
                Map<String, String> indexes;
                try {
                    rs = metaData.getIndexInfo(sName, sName, tName, false, true);
                    indexes = new HashMap<String, String>();
                    while (rs.next()) {
                        String columnName = rs.getString(9);
                        String indexName = rs.getString(6);
                        if (columnName != null && indexName != null) {
                            indexes.put(columnName, indexName);
                        }
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }
                return indexes;
            }
        });
    }

    /**
     * <pre>
     * 常见的物化视图创建语句：
     * 1. CREATE MATERIALIZED VIEW LOG ON test_all_target with primary key;
     *
     * 本方法，主要提取生成物化视图的表名
     * </pre>
     *
     * @param dataSource
     * @param schemaName
     * @param tableName
     * @return
     */
    public static String getMLogTableName(final DataSource dataSource, final String schemaName, final String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String querySql = StringUtils.isEmpty(schemaName) ? mlogQuerySql : mlogSchemaQuerySql;

        return (String) jdbcTemplate.execute(querySql, new PreparedStatementCallback() {

            public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                DatabaseMetaData metaData = ps.getConnection().getMetaData();
                String sName = getIdentifierName(schemaName, metaData);
                String tName = getIdentifierName(tableName, metaData);

                ps.setString(1, tName);
                if (StringUtils.isNotEmpty(schemaName)) {
                    ps.setString(2, sName);
                }

                ResultSet rs = null;
                String log;
                try {
                    rs = ps.executeQuery();
                    log = null;
                    if (rs.next()) {
                        log = rs.getString("log_table");
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }
                return log;
            }
        });
    }

    public static void buildColumns(DataSource dataSource, final Table table) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(new ConnectionCallback() {

            public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet rs = null;
                // 查询所有字段
                List<ColumnMeta> columnList;
                try {
                    rs = metaData.getColumns(table.getSchema(), table.getSchema(), table.getName(), null);
                    columnList = new ArrayList<ColumnMeta>();

                    while (rs.next()) {
                        String catlog = rs.getString(1);
                        String schema = rs.getString(2);
                        String name = rs.getString(3);
                        if ((table.getSchema() == null || LikeUtil.isMatch(table.getSchema(), catlog) || LikeUtil.isMatch(table.getSchema(),
                                schema))
                                && LikeUtil.isMatch(table.getName(), name)) {
                            String columnName = rs.getString(4); // COLUMN_NAME
                            int columnType = rs.getInt(5);
                            String typeName = rs.getString(6);
                            columnType = convertSqlType(columnType, typeName);
                            ColumnMeta col = new ColumnMeta(columnName, columnType);
                            columnList.add(col);
                        }
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }

                // 查询主键信息
                List<String> primaryKeys;
                try {
                    rs = metaData.getPrimaryKeys(table.getSchema(), table.getSchema(), table.getName());
                    primaryKeys = new ArrayList<String>();
                    while (rs.next()) {
                        String catlog = rs.getString(1);
                        String schema = rs.getString(2);
                        String name = rs.getString(3);
                        if ((table.getSchema() == null || StringUtils.equalsIgnoreCase(catlog, table.getSchema()) || StringUtils.equalsIgnoreCase(schema,
                                table.getSchema()))
                                && StringUtils.equalsIgnoreCase(name, table.getName())) {
                            primaryKeys.add(rs.getString(4));
                        }
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }

                Set<ColumnMeta> columns = new HashSet<ColumnMeta>();
                Set<ColumnMeta> pks = new HashSet<ColumnMeta>();
                for (ColumnMeta columnMeta : columnList) {
                    if (primaryKeys.contains(columnMeta.getName())) {
                        pks.add(columnMeta);
                    } else {
                        columns.add(columnMeta);
                    }
                }

                table.getColumns().addAll(columns);
                table.getPrimaryKeys().addAll(pks);
                return null;
            }

        });

    }

    /**
     * 获取DRDS下表的拆分字段, 返回格式为 id,name
     *
     * @param dataSource
     * @param schemaName
     * @param tableName
     * @return
     */
    public static String getShardKeyByDRDS(final DataSource dataSource, final String schemaName, final String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return (String) jdbcTemplate.execute(queryShardKey, new PreparedStatementCallback() {

            public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                DatabaseMetaData metaData = ps.getConnection().getMetaData();
                // String sName = getIdentifierName(schemaName, metaData);
                String tName = getIdentifierName(tableName, metaData);

                ps.setString(1, tName);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                    String log = null;
                    if (rs.next()) {
                        log = rs.getString("KEYS");
                    }

                    return log;
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }
            }
        });
    }

    /**
     * 根据{@linkplain java.sql.DatabaseMetaData}获取正确的表名
     * <p/>
     * <pre>
     * metaData中的storesUpperCaseIdentifiers，storesUpperCaseQuotedIdentifiers，storesLowerCaseIdentifiers,
     * storesLowerCaseQuotedIdentifiers,storesMixedCaseIdentifiers,storesMixedCaseQuotedIdentifiers
     * </pre>
     *
     * @param name
     * @param metaData
     * @return
     * @throws java.sql.SQLException
     */
    private static String getIdentifierName(String name, DatabaseMetaData metaData) throws SQLException {
        if (metaData.storesMixedCaseIdentifiers()) {
            return name; // 保留原始名
        } else if (metaData.storesUpperCaseIdentifiers()) {
            return StringUtils.upperCase(name);
        } else if (metaData.storesLowerCaseIdentifiers()) {
            return StringUtils.lowerCase(name);
        } else {
            return name;
        }
    }

    private static int convertSqlType(int columnType, String typeName) {
        String[] typeSplit = typeName.split(" ");
        if (typeSplit.length > 1) {
            if (columnType == Types.INTEGER && StringUtils.equalsIgnoreCase(typeSplit[1], "UNSIGNED")) {
                columnType = Types.BIGINT;
            }
        }

        if (columnType == Types.OTHER) {
            if (StringUtils.equalsIgnoreCase(typeName, "NVARCHAR")
                    || StringUtils.equalsIgnoreCase(typeName, "NVARCHAR2")) {
                columnType = Types.VARCHAR;
            }

            if (StringUtils.equalsIgnoreCase(typeName, "NCLOB")) {
                columnType = Types.CLOB;
            }

            if (StringUtils.startsWithIgnoreCase(typeName, "TIMESTAMP")) {
                columnType = Types.TIMESTAMP;
            }
        }
        return columnType;
    }

}
