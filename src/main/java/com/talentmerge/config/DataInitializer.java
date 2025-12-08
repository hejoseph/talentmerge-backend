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

        if (!candidateRepository.existsByEmail("candidate@me.com")) {
            Candidate candidate = new Candidate();
            candidate.setName("John Alexander Smith");
            candidate.setEmail("candidate@me.com");
            candidate.setPhone("+1-555-123-4567");
            candidate.setSkills("Java, Spring Boot, React, JavaScript, TypeScript, PostgreSQL, Docker, AWS, Git, Agile, Scrum, REST APIs, Microservices, JUnit, Maven");

            // Create work experiences
            WorkExperience exp1 = new WorkExperience();
            exp1.setJobTitle("Senior Full Stack Developer");
            exp1.setCompany("TechCorp Solutions");
            exp1.setStartDate(LocalDate.of(2021, 3, 15));
            exp1.setEndDate(null); // Current position
            exp1.setDescription("Lead development of microservices architecture using Spring Boot and React. Managed team of 4 developers. Implemented CI/CD pipelines and improved system performance by 40%. Built RESTful APIs serving 10M+ requests daily.");
            candidate.addWorkExperience(exp1);

            WorkExperience exp2 = new WorkExperience();
            exp2.setJobTitle("Full Stack Developer");
            exp2.setCompany("Digital Innovations Inc");
            exp2.setStartDate(LocalDate.of(2019, 6, 1));
            exp2.setEndDate(LocalDate.of(2021, 3, 10));
            exp2.setDescription("Developed responsive web applications using React and Spring framework. Collaborated with UX/UI team to implement modern designs. Integrated third-party APIs and payment gateways. Maintained 99.9% uptime for production systems.");
            candidate.addWorkExperience(exp2);

            WorkExperience exp3 = new WorkExperience();
            exp3.setJobTitle("Junior Software Developer");
            exp3.setCompany("StartupTech Ltd");
            exp3.setStartDate(LocalDate.of(2017, 8, 15));
            exp3.setEndDate(LocalDate.of(2019, 5, 30));
            exp3.setDescription("Built web applications using JavaScript, HTML, CSS, and Node.js. Participated in agile development process. Created automated tests and debugging procedures. Supported legacy system maintenance and feature updates.");
                    candidate.addWorkExperience(exp3);

            // Create education entries
            Education edu1 = new Education();
            edu1.setInstitution("Massachusetts Institute of Technology");
            edu1.setDegree("Master of Science in Computer Science");
            edu1.setGraduationDate(LocalDate.of(2017, 5, 20));
            candidate.addEducation(edu1);

            Education edu2 = new Education();
            edu2.setInstitution("University of California, Berkeley");
            edu2.setDegree("Bachelor of Science in Software Engineering");
            edu2.setGraduationDate(LocalDate.of(2015, 6, 15));
            candidate.addEducation(edu2);

            candidateRepository.save(candidate);
            System.out.println("Created sample candidate: " + candidate.getEmail());
        }
    }
}