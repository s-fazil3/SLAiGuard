package com.sla.backend.dto;

import java.time.LocalDateTime;

public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime localDateTime;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.localDateTime = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }
}
