package com.bella.cango.instance.oracle.common.model;


import com.bella.cango.enums.DbType;

import javax.sql.DataSource;

/**
 * yugong数据处理上下文
 *
 * @author kevin
 */
public class YuGongContext {

    // 具体每张表的同步
    private boolean ignoreSchema = false;  // 同步时是否忽略schema，oracle迁移到mysql可能schema不同，可设置为忽略

    // 全局共享
    private RunMode runMode;
    private int onceCrawNum;                   // 每次提取的记录数
    private int tpsLimit = 0;      // <=0代表不限制
    private DataSource sourceDs;                      // 源数据库链接
    private boolean batchApply = false;
    private boolean skipApplierException = false;  // 是否允许跳过applier异常
    private String sourceEncoding = "UTF-8";
    private DbType sourceDbType = DbType.ORACLE;

    private String dbName;
    private String dbHost;
    private Integer dbPort;
    private String name;

    public int getOnceCrawNum() {
        return onceCrawNum;
    }

    public void setOnceCrawNum(int onceCrawNum) {
        this.onceCrawNum = onceCrawNum;
    }

    public DataSource getSourceDs() {
        return sourceDs;
    }

    public void setSourceDs(DataSource sourceDs) {
        this.sourceDs = sourceDs;
    }


    public boolean isBatchApply() {
        return batchApply;
    }

    public void setBatchApply(boolean batchApply) {
        this.batchApply = batchApply;
    }

    public String getSourceEncoding() {
        return sourceEncoding;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public int getTpsLimit() {
        return tpsLimit;
    }

    public void setTpsLimit(int tpsLimit) {
        this.tpsLimit = tpsLimit;
    }

    public RunMode getRunMode() {
        return runMode;
    }

    public void setRunMode(RunMode runMode) {
        this.runMode = runMode;
    }

    public boolean isIgnoreSchema() {
        return ignoreSchema;
    }

    public void setIgnoreSchema(boolean ignoreSchema) {
        this.ignoreSchema = ignoreSchema;
    }

    public boolean isSkipApplierException() {
        return skipApplierException;
    }

    public void setSkipApplierException(boolean skipApplierException) {
        this.skipApplierException = skipApplierException;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public Integer getDbPort() {
        return dbPort;
    }

    public void setDbPort(Integer dbPort) {
        this.dbPort = dbPort;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public DbType getSourceDbType() {
        return sourceDbType;
    }

    public void setSourceDbType(DbType sourceDbType) {
        this.sourceDbType = sourceDbType;
    }

}
