package com.bella.cango.dto;

import com.bella.cango.enums.DbType;
import com.bella.cango.validate.SlaveId;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/7
 */
@SlaveId
public class CangoRequestDto {
    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = -3153523853123787010L;

    /**
     * The Name.
     *
     * @描述:canal实例名称
     * @字段:name VARCHAR(50)
     */
    @NotEmpty
    private String name;


    /**
     * The Host.
     *
     * @描述:源库主机地址
     * @字段:host VARCHAR(50)
     */
    @NotEmpty
    private String host;


    /**
     * The Port.
     *
     * @描述:源库主机端口
     * @字段:port INT(10)
     */
    @NotNull
    private Integer port;


    /**
     * The User name.
     *
     * @描述:源库用户名
     * @字段:user_name VARCHAR(100)
     */
    @NotEmpty
    private String userName;


    /**
     * The Password.
     *
     * @描述:源库密码
     * @字段:password VARCHAR(32)
     */
    @NotEmpty
    private String password;

    /**
     * @描述:源库名称
     * @字段:db_name VARCHAR(50)
     */
    @NotEmpty
    private String dbName;

    /**
     * 该Canal实例上所有感兴趣的表.
     */
    private Set<String> tableNames;

    /**
     * The Db type.
     *
     */
    @NotNull
    private DbType dbType;


    /**
     * 模拟mysql从库
     *
     */
    @NotNull
    private Integer slaveId;

    /**
     * 需要过滤的表库.
     *
     */
    private String blackTables;

//    /**
//     * binlog解析时间.
//     */
//    private Date logParseTime;
//
//    /**
//     * Gets log parse time.
//     *
//     * @return the log parse time
//     *
//     */
//    public Date getLogParseTime() {
//        return logParseTime;
//    }
//
//    /**
//     * Sets log parse time.
//     *
//     * @param logParseTime the log parse time
//     *
//     */
//    public CangoRequestDto setLogParseTime(Date logParseTime) {
//        this.logParseTime = logParseTime;
//        return this;
//    }

    /**
     * Gets db type.
     *
     * @return the db type
     */
    public DbType getDbType() {
        return dbType;
    }

    /**
     * Sets db type.
     *
     * @param dbType the db type
     * @return the db type
     */
    public CangoRequestDto setDbType(DbType dbType) {
        this.dbType = dbType;
        return this;
    }

    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets host.
     *
     * @param host the host
     * @return the host
     */
    public CangoRequestDto setHost(String host) {
        this.host = host;
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
    public CangoRequestDto setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets password.
     *
     * @param password the password
     * @return the password
     */
    public CangoRequestDto setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Sets port.
     *
     * @param port the port
     * @return the port
     */
    public CangoRequestDto setPort(Integer port) {
        this.port = port;
        return this;
    }

    /**
     * Gets slave id.
     *
     * @return the slave id
     */
    public Integer getSlaveId() {
        return slaveId;
    }

    /**
     * Sets slave id.
     *
     * @param slaveId the slave id
     * @return the slave id
     * @date :2016-04-21 11:40:42
     */
    public CangoRequestDto setSlaveId(Integer slaveId) {
        this.slaveId = slaveId;
        return this;
    }

    /**
     * Gets user name.
     *
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets user name.
     *
     * @param userName the user name
     * @return the user name
     */
    public CangoRequestDto setUserName(String userName) {
        this.userName = userName;
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
     * @date :2016-05-03 13:51:16
     */
    public CangoRequestDto setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    /**
     * Gets table names.
     *
     * @return the table names
     */
    public Set<String> getTableNames() {
        return tableNames;
    }

    /**
     * Sets table names.
     *
     * @param tableNames the table names
     * @return the table names
     */
    public CangoRequestDto setTableNames(Set<String> tableNames) {
        this.tableNames = tableNames;
        return this;
    }

    /**
     * Gets black tables.
     *
     * @return the black tables
     */
    public String getBlackTables() {
        return blackTables;
    }

    /**
     * Sets black tables.
     *
     * @param blackTables the black tables
     * @return the black tables
     */
    public CangoRequestDto setBlackTables(String blackTables) {
        this.blackTables = blackTables;
        return this;
    }
}
