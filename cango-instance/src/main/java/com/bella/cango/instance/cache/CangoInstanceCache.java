package com.bella.cango.instance.cache;


import com.bella.cango.instance.mysql.MysqlInstance;
import com.bella.cango.instance.oracle.OracleInstance;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/16
 */
public class CangoInstanceCache {
    private static final ConcurrentHashMap<String, MysqlInstance> mysqlInstanceMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, OracleInstance> oracleInstanceMap = new ConcurrentHashMap<>();

    public static void addOracleInstance(String key, OracleInstance oracleInstance) {
        oracleInstanceMap.putIfAbsent(key, oracleInstance);
    }

    public static void addMysqlInstance(String key, MysqlInstance mysqlInstance) {
        mysqlInstanceMap.putIfAbsent(key, mysqlInstance);
    }

    public static Map<String, MysqlInstance> getMysqlInstanceMap() {
        return Collections.unmodifiableMap(mysqlInstanceMap);
    }

    public static Map<String, OracleInstance> getOracleInstanceMap() {
        return Collections.unmodifiableMap(oracleInstanceMap);
    }


    public static void removeOracleInstance(String key) {
        oracleInstanceMap.remove(key);
    }

    public static void removeMysqlInstance(String key) {
        mysqlInstanceMap.remove(key);
    }

    public static void clearCache() {
        oracleInstanceMap.clear();
        mysqlInstanceMap.clear();
    }

    public static String createKey(String dbHost, int dbPort) {
        return String.format("%s_%d", dbHost, dbPort);
    }

    public static MysqlInstance getMysqlInstance(String key) {
        return mysqlInstanceMap.get(key);
    }

    public static OracleInstance getOracleInstance(String key) {
        return oracleInstanceMap.get(key);
    }
}
