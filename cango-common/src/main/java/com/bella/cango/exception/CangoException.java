package com.bella.cango.exception;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/21
 */
public class CangoException extends RuntimeException {
    public CangoException() {
        super();
    }

    public CangoException(String message) {
        super(message);
    }

    public CangoException(String message, Throwable cause) {
        super(message, cause);
    }

    public CangoException(Throwable cause) {
        super(cause);
    }

    protected CangoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
