package com.fadaiting.camp.expection;

public class AccountConflictException extends RuntimeException {
    public AccountConflictException(String message) { super(message); }
}
