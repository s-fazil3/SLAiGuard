package com.sla.backend.dto;

public class LoginResponse {
    private String token;
    private String type;

    public LoginResponse(String token) {
        this.token = token;
        this.type = "Bearer";
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }
}
