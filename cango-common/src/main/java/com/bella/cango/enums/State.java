package com.bella.cango.enums;

/**
 * The enum State.
 */
public enum State {

    /**
     * 启动实例.
     */
    START(1, "启动"),

    /**
     * 停用实例
     */
    STOP(2, "停用"),

    /**
     * 禁用实例
     */
    DISABLE(3, "禁用");

    /**
     * The Code.
     */
    private Integer code;

    /**
     * The Desc.
     */
    private String desc;

    /**
     * Instantiates a new Canal rsp status.
     *
     * @param code the code
     * @param desc the desc
     */
    private State(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * Value of canal rsp status.
     *
     * @param code the code
     * @return the canal rsp status
     * @date :2016-04-25 14:13:28
     */
    public static State valueOf(Integer code) {
        if (code != null) {
            State[] values = values();
            for (State value : values) {
                if (code.equals(value.code)) {
                    return value;
                }
            }

        }
        return null;
    }

    /**
     * Gets code.
     *
     * @return the code
     * @date :2016-04-25 14:13:28
     */
    public Integer getCode() {
        return code;
    }

    /**
     * Sets code.
     *
     * @param code the code
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * Gets desc.
     *
     * @return the desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Sets desc.
     *
     * @param desc the desc
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

}
