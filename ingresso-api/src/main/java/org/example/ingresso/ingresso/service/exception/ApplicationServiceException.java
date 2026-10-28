package org.example.ingresso.ingresso.service.exception;

public class ApplicationServiceException extends RuntimeException {

    private final Code code;

    public ApplicationServiceException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public enum Code {
        NOT_FOUND,
        UNAUTHORIZED,
        FORBIDDEN
    }
}