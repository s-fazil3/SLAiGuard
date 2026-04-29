package com.sla.backend.service;

import com.sla.backend.entity.User;
import com.sla.backend.repository.UserRepository;
import com.sla.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,PasswordEncoder passwordEncoder,JwtUtil jwtUtil){
        this.passwordEncoder=passwordEncoder;
        this.userRepository=userRepository;
        this.jwtUtil=jwtUtil;
    }

    public void signup(String username,String email,String rawpassword){
        if(userRepository.existsByEmailIgnoreCase(email)){
            throw new RuntimeException("Email already exists");
        }

        String encoded=passwordEncoder.encode(rawpassword);
        User user=new User(username,encoded,"ROLE_USER",email);
        userRepository.save(user);
    }

    public String login(String email,String password){
        System.out.println("DEBUG: Attempting login for email: " + email);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    System.out.println("DEBUG: User not found for email: " + email);
                    return new RuntimeException("Invalid credentials");
                });

        System.out.println("DEBUG: Found user: " + user.getUsername() + ", role: " + user.getRole());
        System.out.println("DEBUG: Password match result: " + passwordEncoder.matches(password, user.getPassword()));
        
        if(!passwordEncoder.matches(password,user.getPassword())){
            System.out.println("DEBUG: Password mismatch for email: " + email);
            throw new RuntimeException("Invalid credentials");
        }
        
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        System.out.println("DEBUG: Login successful for email: " + email);
        return token;
    }
}
