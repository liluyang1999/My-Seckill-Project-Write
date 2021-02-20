package com.example.lly.exception;

import com.example.lly.util.BaseUtil;

import java.io.Serial;

public class FailedSeckillException extends BaseSeckillException {

    private static final String message = "活动已结束！";
    @Serial
    private static final long serialVersionUID = BaseUtil.SERIAL_VERSION_UID;

    public FailedSeckillException() {
        super(message);
    }

    public FailedSeckillException(String message) {
        super(message);
    }

    public FailedSeckillException(String message, Throwable cause) {
        super(message, cause);
    }

    public static String getMsg() {
        return message;
    }

}
