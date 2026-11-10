package com.fadaiting.camp.expection;
public class NotificationDeliveryException extends RuntimeException {
    private final String errorCode;
    private final boolean retryable;
    public NotificationDeliveryException(String errorCode, boolean retryable) { super("通知未完成，請核對通知嘗試紀錄"); this.errorCode=errorCode; this.retryable=retryable; }
    public String getErrorCode() { return errorCode; }
    public boolean isRetryable() { return retryable; }
}
