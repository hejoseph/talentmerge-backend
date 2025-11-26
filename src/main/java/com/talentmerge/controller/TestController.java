package com.talentmerge.controller;

import com.talentmerge.model.User;
import com.talentmerge.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @GetMapping("/check-password/{username}")
    public String checkPassword(@PathVariable String username, @RequestParam String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "User not found";
        }
        
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        return "Password match: " + matches + ", Stored hash: " + user.getPassword().substring(0, 20) + "...";
    }
}