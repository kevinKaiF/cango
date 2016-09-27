package com.bella.cango.client;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/16
 */
public enum RequestCommand {
    START("start"),
    STOP("stop"),
    ENABLE("enable"),
    DISABLE("disable"),
    STARTALL("startAll"),
    STOPALL("stopAll"),
    SHUTDOWN("shutdown"),
    CLEARALL("clearAll"),
    CHECK("check"),
    ADD("add");

    private String command;

    RequestCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
