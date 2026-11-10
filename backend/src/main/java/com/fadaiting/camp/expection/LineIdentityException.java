package com.fadaiting.camp.expection;

public class LineIdentityException extends RuntimeException {
    private final boolean unavailable;
    public LineIdentityException(boolean unavailable) {
        super(unavailable ? "LINE 身分驗證服務尚未設定或暫時無法使用，請改用網站查單" : "LINE 身分驗證失敗，請重新登入 LINE");
        this.unavailable = unavailable;
    }
    public boolean isUnavailable() { return unavailable; }
}
