package com.sla.backend.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Test passwords from DB
        String password1 = "admin1";
        String hash1 = "$2a$10$f/1iHltOfC8DSY5LuUrUkuALGHjerfKfUrz0NqVZfoHiElGyR4xRO";
        
        String password2 = "zayn";
        String hash2 = "$2a$10$CNEuITv.25rWQiG7cHsh4eeKfwoGXxWz1m/NzT.LnANXK1OwoaA1e";
        
        String password3 = "vaishnav";
        String hash3 = "$2a$10$HO.9mNzrci1BtdblHdW/b.GP9h7ctDDSwf2ce0vcPyy9zqlpQdhzq";
        
        System.out.println("admin1 matches: " + encoder.matches(password1, hash1));
        System.out.println("zayn matches: " + encoder.matches(password2, hash2));
        System.out.println("vaishnav matches: " + encoder.matches(password3, hash3));
        
        // Test new encoding
        String newHash = encoder.encode("admin1");
        System.out.println("New hash for admin1: " + newHash);
    }
}
