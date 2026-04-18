package com.example.chat.common;

public class FieldException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String field;

    public FieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
