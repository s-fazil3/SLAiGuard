package com.sla.backend.controller;

import com.sla.backend.dto.ProfileResponse;
import com.sla.backend.entity.User;
import com.sla.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ProfileResponse getProfile(Authentication authentication){
        // Temporarily return a demo user profile for demo purposes
        if (authentication == null) {
            // Return demo profile when not authenticated
            return new ProfileResponse("Demo Admin", "admin@sla-demo.com", "ROLE_ADMIN");
        }

        String email=authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String role=authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return new ProfileResponse(user.getUsername(), email,role);
    }
}
