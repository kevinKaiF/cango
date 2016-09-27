package com.bella.cango.dto;

import com.bella.cango.enums.CangoRspStatus;

import java.io.Serializable;

/**
 * The type Cango response dto.
 */
public class CangoResponseDto implements Serializable {

    @Override
    public String toString() {
        return "CanalRspDto [ failMsg=" + failMsg + ", msg=" + msg + "]";
    }

    private static final long serialVersionUID = -5348804938984020686L;

    /**
     * 失败信息.
     */
    private String failMsg;

    /**
     * 成功的响应信息.
     */
    private String msg;

    private CangoRspStatus status;

    /**
     * Gets fail msg.
     *
     * @return the fail msg
     * @date :2016-04-25 14:14:31
     */
    public String getFailMsg() {
        return failMsg;
    }

    /**
     * Sets fail msg.
     *
     * @param failMsg the fail msg
     * @date :2016-04-25 14:14:31
     */
    public void setFailMsg(String failMsg) {
        this.failMsg = failMsg;
    }


    /**
     * Gets msg.
     *
     * @return the msg
     * @date :2016-04-27 18:36:03
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Sets msg.
     *
     * @param msg the msg
     * @return the msg
     * @date :2016-04-27 18:36:03
     */
    public CangoResponseDto setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public CangoRspStatus getStatus() {
        return status;
    }

    public CangoResponseDto setStatus(CangoRspStatus status) {
        this.status = status;
        return this;
    }
}
