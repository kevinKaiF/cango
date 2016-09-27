package com.bella.cango.enums;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/7
 */
public enum DbType {
    MYSQL(1, "com.mysql.jdbc.Driver"),
    ORACLE(2, "oracle.jdbc.driver.OracleDriver"),
    DRDS(3, "com.mysql.jdbc.Driver");

    private Integer code;
    private String driver;

    DbType(Integer type, String driver) {
        this.code = type;
    }

    public Integer getCode() {
        return code;
    }

    public String getDriver() {
        return driver;
    }

    public boolean isOracle() {
        return this.equals(ORACLE);
    }


    public boolean isMysql() {
        return this.equals(MYSQL);
    }

    public boolean isDRDS() {
        return this.equals(DRDS);
    }

    public DbType parse(Integer code) {
        switch (code) {
            case 1:
                return MYSQL;
            case 2:
                return ORACLE;
            case 3:
                return DRDS;
            default:
                return null;
        }
    }
}
