package com.talentmerge.controller;

import com.talentmerge.dto.ErrorResponse;
import com.talentmerge.dto.LoginRequest;
import com.talentmerge.dto.LoginResponse;
import com.talentmerge.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                            HttpServletRequest request) {
        try {
            LoginResponse loginResponse = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(loginResponse);
        } catch (BadCredentialsException e) {
            ErrorResponse errorResponse = new ErrorResponse(401, "Unauthorized", 
                    "Invalid username or password", request.getRequestURI());
            return ResponseEntity.status(401).body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(500, "Internal Server Error", 
                    "An unexpected error occurred", request.getRequestURI());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("TalentMerge API is running!");
    }
}