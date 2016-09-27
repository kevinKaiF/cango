package com.bella.cango.entity;

import java.util.Date;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/30
 */
public class CangoTable {

    private Long id;

    private String instancesName;

    private String tableName;

    private Date createTime;

    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstancesName() {
        return instancesName;
    }

    public CangoTable setInstancesName(String instancesName) {
        this.instancesName = instancesName;
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public CangoTable setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public CangoTable setCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public CangoTable setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }
}
