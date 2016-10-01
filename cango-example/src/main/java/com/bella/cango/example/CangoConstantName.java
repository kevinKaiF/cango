package com.bella.cango.example;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/10/1
 */
public enum  CangoConstantName {
    DB1("49a25b35-c7ec-4fd6-a2ec-b51982aeff06"),
    DB2("e7af7237-6010-4e09-b32d-cf10f9cb73d6");

    private String name;

    CangoConstantName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
