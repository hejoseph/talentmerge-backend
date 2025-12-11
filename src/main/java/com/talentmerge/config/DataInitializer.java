package com.talentmerge.config;

import com.talentmerge.model.*;
import com.talentmerge.repository.CandidateRepository;
import com.talentmerge.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateRepository candidateRepository;
    
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

        // Fetch users to assign ownership
        User adminUser = userRepository.findByUsername("admin").orElse(null);
        User testUser = userRepository.findByUsername("testuser").orElse(null);

        // Seed candidates for admin
        if (adminUser != null && !candidateRepository.existsByEmail("admin.candidate1@talentmerge.com")) {
            Candidate c1 = new Candidate();
            c1.setOwner(adminUser);
            c1.setName("Admin Candidate One");
            c1.setEmail("admin.candidate1@talentmerge.com");
            c1.setPhone("+1-555-111-1111");
            c1.setSkills("Java, Spring Boot, AWS");

            WorkExperience a1 = new WorkExperience();
            a1.setJobTitle("Backend Engineer");
            a1.setCompany("Acme Corp");
            a1.setStartDate(LocalDate.of(2020,1,1));
            a1.setEndDate(null);
            a1.setDescription("Building APIs");
            c1.addWorkExperience(a1);

            Education ae1 = new Education();
            ae1.setInstitution("State University");
            ae1.setDegree("B.Sc. Computer Science");
            ae1.setGraduationDate(LocalDate.of(2018,6,1));
            c1.addEducation(ae1);

            candidateRepository.save(c1);
        }

        if (adminUser != null && !candidateRepository.existsByEmail("admin.candidate2@talentmerge.com")) {
            Candidate c2 = new Candidate();
            c2.setOwner(adminUser);
            c2.setName("Admin Candidate Two");
            c2.setEmail("admin.candidate2@talentmerge.com");
            c2.setPhone("+1-555-222-2222");
            c2.setSkills("React, Node.js, DevOps");
            candidateRepository.save(c2);
        }

        // Seed candidates for testuser
        if (testUser != null && !candidateRepository.existsByEmail("testuser.candidate1@talentmerge.com")) {
            Candidate c3 = new Candidate();
            c3.setOwner(testUser);
            c3.setName("Testuser Candidate One");
            c3.setEmail("testuser.candidate1@talentmerge.com");
            c3.setPhone("+1-555-333-3333");
            c3.setSkills("Python, Django, PostgreSQL");
            candidateRepository.save(c3);
        }

        if (testUser != null && !candidateRepository.existsByEmail("testuser.candidate2@talentmerge.com")) {
            Candidate c4 = new Candidate();
            c4.setOwner(testUser);
            c4.setName("Testuser Candidate Two");
            c4.setEmail("testuser.candidate2@talentmerge.com");
            c4.setPhone("+1-555-444-4444");
            c4.setSkills("Kotlin, Android, Firebase");
            candidateRepository.save(c4);
        }
    }
}