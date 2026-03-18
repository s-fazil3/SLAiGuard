package com.sla.backend.controller;
import com.sla.backend.dto.LoginRequest;
import com.sla.backend.dto.LoginResponse;
import com.sla.backend.dto.SignupRequest;
import com.sla.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token= authService.login(
                request.getEmail(),
                request.getPassword()
        );
        return ResponseEntity.ok(new LoginResponse(token));
    }
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok("User registered successfully");
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logged out successfully");
    }

}