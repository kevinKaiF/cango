package com.bella.cango.enums;

public enum EventType {

    /**
     * Insert event type.
     */
    INSERT(1, "insert record"),
    /**
     * Update event type.
     */
    UPDATE(2, "update record"),
    /**
     * Delete event type.
     */
    DELETE(3, "delete record"),
    /**
     * Create event type.
     */
    CREATE(4, "create table"),
    /**
     * Alter event type.
     */
    ALTER(5, "alter table"),
    /**
     * Erase event type.
     */
    ERASE(6, "erase table");

    /**
     * The Code.
     */
    private Integer code;

    /**
     * The Desc.
     */
    private String desc;

    /**
     * Instantiates a new Event type.
     *
     * @param code the code
     * @param desc the desc
     */
    private EventType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * Value of event type.
     *
     * @param code the code
     * @return the event type
     */
    public static EventType valueOf(Integer code) {
        if (code != null) {
            EventType[] values = values();
            for (EventType value : values) {
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
