package com.bella.cango.dto;

import java.io.Serializable;

public class ColumnDto implements Serializable {
    private static final long serialVersionUID = 7309111666343868682L;

    /**
     * The Name.
     */
    private String name;

    /**
     * 操作后的字符值.
     */
    private String strValue;

    /**
     * 操作后的值（已按数据库类型(jdbcType)转为对应的Java类型）.
     */
    private Object value;

    /**
     * 操作前的字符值.
     */
    private String oldStrValue;

    /**
     * 操作前的值（已按数据库类型转为对应的Java类型）
     */
    private Object oldValue;

    /**
     * The Jdbc type.
     */
    private int jdbcType;

    public int getJdbcType() {
        return jdbcType;
    }

    public ColumnDto setJdbcType(int jdbcType) {
        this.jdbcType = jdbcType;
        return this;
    }

    public String getName() {
        return name;
    }

    public ColumnDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getStrValue() {
        return strValue;
    }

    public ColumnDto setStrValue(String strValue) {
        this.strValue = strValue;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public ColumnDto setValue(Object value) {
        this.value = value;
        return this;
    }

    public String getOldStrValue() {
        return oldStrValue;
    }

    public ColumnDto setOldStrValue(String oldStrValue) {
        this.oldStrValue = oldStrValue;
        return this;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public ColumnDto setOldValue(Object oldValue) {
        this.oldValue = oldValue;
        return this;
    }

}
