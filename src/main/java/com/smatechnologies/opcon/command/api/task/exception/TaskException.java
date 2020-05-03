package com.smatechnologies.opcon.command.api.task.exception;

public class TaskException extends Exception {

    private final int code;

    public TaskException(String message) {
        this(message, 1);
    }

    public TaskException(String message, int code) {
        super(message);
        this.code = code;
    }

    public TaskException(String message, Throwable cause) {
        this(message, cause, 1);
    }

    public TaskException(Throwable cause) {
        this(cause.getMessage(), cause, 1);
    }

    public TaskException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
