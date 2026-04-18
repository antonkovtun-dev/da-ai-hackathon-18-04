package com.example.chat.auth;

public class FieldException extends RuntimeException {
    private final String field;

    public FieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
