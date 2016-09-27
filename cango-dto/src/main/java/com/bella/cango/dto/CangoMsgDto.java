package com.bella.cango.dto;


import com.bella.cango.enums.EventType;

import java.util.List;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/7
 */
public class CangoMsgDto {
    /**
     * 序列化ID，要保证兼容性时不要修改此值
     */
    private static final long serialVersionUID = -1655844036921106774L;

    /**
     * Cango实例名称.
     */
    protected String name;

    /**
     * 变更的数据库地址
     */
    protected String dbHost;

    /**
     * 变更的数据库端口
     */
    protected Integer dbPort;

    /**
     * 变更的数据库名称
     */
    protected String dbName;

    /**
     * 变更的表名
     */
    protected String tableName;

    /**
     * 变更类型
     */
    protected EventType eventType;

    /**
     * 变更行的主键名称.
     */
    protected String pkName;

    /**
     * 变更行的主键值.
     */
    protected Object pkValue;

    /**
     * 执行时间.
     */
    protected Long executeTime;

    /**
     * 所有列数据，如果添加Canal实例时订阅了所有列数据，则返回的DTO中包含此数据。
     */
    protected List<ColumnDto> columns;

    /**
     * Gets db host.
     *
     * @return the db host
     */
    public String getDbHost() {
        return dbHost;
    }

    /**
     * Sets db host.
     *
     * @param dbHost the db host
     * @return the db host
     */
    public CangoMsgDto setDbHost(String dbHost) {
        this.dbHost = dbHost;
        return this;
    }

    /**
     * Gets db name.
     *
     * @return the db name
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Sets db name.
     *
     * @param dbName the db name
     * @return the db name
     */
    public CangoMsgDto setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    /**
     * Gets db port.
     *
     * @return the db port
     */
    public Integer getDbPort() {
        return dbPort;
    }

    /**
     * Sets db port.
     *
     * @param dbPort the db port
     * @return the db port
     */
    public CangoMsgDto setDbPort(Integer dbPort) {
        this.dbPort = dbPort;
        return this;
    }

    /**
     * Gets event type.
     *
     * @return the event type
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Sets event type.
     *
     * @param eventType the event type
     * @return the event type
     */
    public CangoMsgDto setEventType(EventType eventType) {
        this.eventType = eventType;
        return this;
    }

    /**
     * Gets table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets table name.
     *
     * @param tableName the table name
     * @return the table name
     */
    public CangoMsgDto setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    /**
     * Gets pk name.
     *
     * @return the pk name
     */
    public String getPkName() {
        return pkName;
    }

    /**
     * Sets pk name.
     *
     * @param pkName the pk name
     * @return the pk name
     */
    public CangoMsgDto setPkName(String pkName) {
        this.pkName = pkName;
        return this;
    }

    /**
     * Gets pk value.
     *
     * @return the pk value
     */
    public Object getPkValue() {
        return pkValue;
    }

    /**
     * Sets pk value.
     *
     * @param pkValue the pk value
     * @return the pk value
     */
    public CangoMsgDto setPkValue(Object pkValue) {
        this.pkValue = pkValue;
        return this;
    }


    /**
     * Gets columns.
     *
     * @return the columns
     */
    public List<ColumnDto> getColumns() {
        return columns;
    }

    /**
     * Sets columns.
     *
     * @param columns the columns
     * @return the columns
     */
    public CangoMsgDto setColumns(List<ColumnDto> columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     * @return the name
     */
    public CangoMsgDto setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets execute time.
     *
     * @return the execute time
     */
    public Long getExecuteTime() {
        return executeTime;
    }

    /**
     * Sets execute time.
     *
     * @param executeTime the execute time
     * @return the execute time
     */
    public CangoMsgDto setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
        return this;
    }
}
