package com.it;

public class ITException extends RuntimeException {

    public ITException(String message) {
        super(message);
    }

    public ITException(String message, Throwable cause) {
        super(message, cause);
    }
}
