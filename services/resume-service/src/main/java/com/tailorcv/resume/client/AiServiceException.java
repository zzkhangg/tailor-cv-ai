package com.tailorcv.resume.client;

public class AiServiceException extends RuntimeException {
    private final int status;

    public AiServiceException(int status, String message) {
        super(message);
        this.status = status;
    }

    public AiServiceException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
