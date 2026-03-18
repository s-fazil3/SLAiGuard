package com.sla.backend.dto;

public class ProfileResponse {
    private String username;
    String email;
    private String role;

    public ProfileResponse(String username,String email, String role) {
        this.username = username;
        this.role = role;
        this.email=email;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getEmail(){
        return email;
    }
}
