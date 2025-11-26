package com.talentmerge.config;

import com.talentmerge.model.Role;
import com.talentmerge.model.User;
import com.talentmerge.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@talentmerge.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }
        
        // Create default test user if not exists
        if (!userRepository.existsByUsername("testuser")) {
            User user = new User();
            user.setUsername("testuser");
            user.setEmail("test@talentmerge.com");
            user.setPassword(passwordEncoder.encode("test123"));
            user.setFirstName("Test");
            user.setLastName("User");
            user.setRole(Role.USER);
            userRepository.save(user);
        }
    }
}