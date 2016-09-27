package com.bella.cango.entity;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

public class CangoInstances {

    /**
     * @描述:主键
     * @字段:id BIGINT(19)
     */
    private Long id;


    /**
     * @描述:cango实例名称
     * @字段:name VARCHAR(50)
     */
    @NotEmpty(message = "实例name不能为空")
    private String name;


    /**
     * @描述:源库主机地址
     * @字段:host VARCHAR(50)
     */
    @NotEmpty(message = "数据库host不能为空")
    private String host;


    /**
     * @描述:源库主机端口
     * @字段:port INT(10)
     */
    @Min(value = 1, message = "数据库port不能为空")
    private Integer port;

    @NotEmpty(message = "数据库名不能为空")
    private String dbName;

    private List<String> tableNames;

    @NotEmpty(message = "用户名不能为空")
    private String userName;

    @NotEmpty(message = "密码不能为空")
    private String password;

    @NotNull(message = "数据库类型不能为空")
    private Integer dbType;

    private Integer slaveId;

    /**
     * 需要过滤的表黑名单.
     */
    private String blackTables;

    private Integer state;

    private Date createTime;

    private Date updateTime;

    public CangoInstances() {
    }

    public CangoInstances(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public CangoInstances setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public CangoInstances setName(String name) {
        this.name = name;
        return this;
    }

    public String getHost() {
        return this.host;
    }

    public CangoInstances setHost(String host) {
        this.host = host;
        return this;
    }

    public Integer getPort() {
        return this.port;
    }

    public CangoInstances setPort(Integer port) {
        this.port = port;
        return this;
    }

    public String getDbName() {
        return this.dbName;
    }

    public CangoInstances setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public CangoInstances setTableNames(List<String> tableNames) {
        this.tableNames = tableNames;
        return this;
    }

    public String getUserName() {
        return this.userName;
    }

    public CangoInstances setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getPassword() {
        return this.password;
    }

    public CangoInstances setPassword(String password) {
        this.password = password;
        return this;
    }
    public Integer getDbType() {
        return this.dbType;
    }

    public CangoInstances setDbType(Integer dbType) {
        this.dbType = dbType;
        return this;
    }

    public Integer getSlaveId() {
        return this.slaveId;
    }

    public CangoInstances setSlaveId(Integer slaveId) {
        this.slaveId = slaveId;
        return this;
    }

    public Integer getState() {
        return this.state;
    }

    public CangoInstances setState(Integer state) {
        this.state = state;
        return this;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public CangoInstances setCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public Date getUpdateTime() {
        return this.updateTime;
    }

    public CangoInstances setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public String getBlackTables() {
        return blackTables;
    }

    public void setBlackTables(String blackTables) {
        this.blackTables = blackTables;
    }
}


