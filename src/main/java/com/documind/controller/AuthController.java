package com.documind.controller;

import com.documind.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String message = authService.register(username, password);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status(
            java.security.Principal principal) {
        if (principal != null) {
            return ResponseEntity.ok(
                Map.of("username", principal.getName(), "loggedIn", "true"));
        }
        return ResponseEntity.ok(Map.of("loggedIn", "false"));
    }
}
