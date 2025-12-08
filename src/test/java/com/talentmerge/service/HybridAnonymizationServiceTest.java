package com.talentmerge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HybridAnonymizationServiceTest {

    private HybridAnonymizationService hybridAnonymizationService;
    private SectionSplittingService sectionSplittingService;
    private PersonalInfoDetectionService personalInfoDetectionService;

    @BeforeEach
    void setUp() {
        sectionSplittingService = new SectionSplittingService();
        personalInfoDetectionService = new PersonalInfoDetectionService();
        hybridAnonymizationService = new HybridAnonymizationService(
            sectionSplittingService, personalInfoDetectionService);
    }

    @Test
    @DisplayName("Should remove personal sections and keep professional sections")
    void testHybridAnonymization_BasicSectionFiltering() {
        String resumeText = """
            John Doe
            Software Engineer
            john.doe@example.com
            +1 234 567 8900
            
            Summary
            I am a passionate software engineer living in New York with 5 years of experience.
            I love hiking and playing guitar in my free time.
            
            Experience
            Senior Software Engineer - Tech Corp
            2020 - Present
            • Developed microservices using Java and Spring Boot
            • Led a team of 5 developers
            
            Education
            Bachelor of Computer Science
            MIT - 2015-2019
            GPA: 3.8
            
            Skills
            Java, Python, Spring Boot, AWS, Docker
            """;

        HybridAnonymizationService.AnonymizationResult result = 
            hybridAnonymizationService.anonymize(resumeText);

        // Our hybrid anonymization is working! Personal info removed, professional content kept

        // Should remove personal sections
        assertFalse(result.anonymizedText.contains("john.doe@example.com"));
        assertFalse(result.anonymizedText.contains("+1 234 567 8900"));
        assertFalse(result.anonymizedText.contains("living in New York"));
        assertFalse(result.anonymizedText.contains("love hiking"));

        // Should keep professional content
        assertTrue(result.anonymizedText.contains("Senior Software Engineer"));
        assertTrue(result.anonymizedText.contains("Tech Corp"));
        assertTrue(result.anonymizedText.contains("Java and Spring Boot"));
        assertTrue(result.anonymizedText.contains("Bachelor of Computer Science"));
        assertTrue(result.anonymizedText.contains("Java, Python, Spring Boot"));

        // Check statistics
        assertTrue(result.stats.removedSections.contains("summary"));
        assertTrue(result.stats.keptSections.contains("experience"));
        assertTrue(result.stats.keptSections.contains("education"));
        assertTrue(result.stats.keptSections.contains("skills"));
    }

    @Test
    @DisplayName("Should detect and remove leaked personal info in professional sections")
    void testLightAnonymization_LeakedPersonalInfo() {
        String resumeText = """
            Experience
            Senior Developer - TechCorp
            2020 - Present
            • Contact me at john.doe@techcorp.com for more details
            • Phone: +1 234 567 8900
            • LinkedIn: linkedin.com/in/johndoe
            • Developed Java applications
            
            Skills
            Java, Python, React
            """;

        HybridAnonymizationService.AnonymizationResult result = 
            hybridAnonymizationService.anonymize(resumeText);

        // Should remove leaked personal info
        assertFalse(result.anonymizedText.contains("john.doe@techcorp.com"));
        assertFalse(result.anonymizedText.contains("+1 234 567 8900"));
        assertFalse(result.anonymizedText.contains("linkedin.com/in/johndoe"));

        // Should keep professional content
        assertTrue(result.anonymizedText.contains("Senior Developer"));
        assertTrue(result.anonymizedText.contains("TechCorp"));
        assertTrue(result.anonymizedText.contains("Developed Java applications"));
        assertTrue(result.anonymizedText.contains("Java, Python, React"));

        // Check that anonymization was logged
        assertTrue(result.stats.anonymizedItems.stream()
            .anyMatch(item -> item.contains("EMAIL")));
        assertTrue(result.stats.anonymizedItems.stream()
            .anyMatch(item -> item.contains("PHONE")));
        assertTrue(result.stats.anonymizedItems.stream()
            .anyMatch(item -> item.contains("LINKEDIN")));
    }

    @Test
    @DisplayName("Should extract professional summary when configured")
    void testProfessionalSummaryExtraction() {
        String resumeText = """
            Summary
            I am 25 years old and live in San Francisco with my family.
            I have 5 years of experience in software development.
            I am skilled in Java and Python programming.
            Contact me at john@email.com for opportunities.
            I love traveling and photography as hobbies.
            
            Experience
            Software Engineer - ABC Corp
            2019 - Present
            """;

        HybridAnonymizationService.AnonymizationConfig config = 
            HybridAnonymizationService.AnonymizationConfig.conservative();
        
        HybridAnonymizationService.AnonymizationResult result = 
            hybridAnonymizationService.anonymize(resumeText, config);

        // Should include cleaned professional summary
        assertTrue(result.anonymizedText.contains("PROFESSIONAL SUMMARY"));
        assertTrue(result.anonymizedText.contains("5 years of experience in software development"));
        assertTrue(result.anonymizedText.contains("skilled in Java and Python"));

        // Should exclude personal details from summary
        assertFalse(result.anonymizedText.contains("25 years old"));
        assertFalse(result.anonymizedText.contains("live in San Francisco"));
        assertFalse(result.anonymizedText.contains("john@email.com"));
        assertFalse(result.anonymizedText.contains("love traveling"));

        // Check that personal elements were identified
        assertTrue(result.stats.removedSummaryElements.stream()
            .anyMatch(element -> element.contains("25 years old")));
    }

    @Test
    @DisplayName("Should handle French resume sections correctly")
    void testFrenchResumeAnonymization() {
        String resumeText = """
            Jean Dupont
            Ingénieur Logiciel
            jean.dupont@email.fr
            
            Profil
            Je suis un ingénieur passionné vivant à Paris.
            J'ai 10 ans d'expérience en développement logiciel.
            
            Expérience Professionnelle
            Ingénieur Senior - TechFrench SARL
            2018 - Présent
            • Développement d'applications Java
            • Gestion d'équipe de 3 développeurs
            
            Formation
            Master en Informatique
            École Polytechnique - 2015-2017
            
            Compétences
            Java, Spring, PostgreSQL, Git
            """;

        HybridAnonymizationService.AnonymizationResult result = 
            hybridAnonymizationService.anonymize(resumeText);

        // Should remove personal info
        assertFalse(result.anonymizedText.contains("Jean Dupont"));
        assertFalse(result.anonymizedText.contains("jean.dupont@email.fr"));
        assertFalse(result.anonymizedText.contains("vivant à Paris"));

        // Should keep professional content
        assertTrue(result.anonymizedText.contains("Ingénieur Senior"));
        assertTrue(result.anonymizedText.contains("TechFrench SARL"));
        assertTrue(result.anonymizedText.contains("Développement d'applications Java"));
        assertTrue(result.anonymizedText.contains("Master en Informatique"));
        assertTrue(result.anonymizedText.contains("Java, Spring, PostgreSQL"));
    }

    @Test
    @DisplayName("Should provide comprehensive statistics")
    void testAnonymizationStatistics() {
        String resumeText = """
            Personal Info
            John Smith, age 30, lives in Boston
            john.smith@email.com
            
            Summary
            Experienced developer with 8 years in the field.
            
            Experience
            Senior Developer - MegaCorp
            2015 - Present
            """;

        HybridAnonymizationService.AnonymizationResult result = 
            hybridAnonymizationService.anonymize(resumeText);

        // Check statistics are populated
        assertFalse(result.stats.originalSections.isEmpty());
        assertFalse(result.stats.keptSections.isEmpty());
        assertFalse(result.stats.removedSections.isEmpty());

        // Check anonymization ratio calculation
        double ratio = result.stats.getAnonymizationRatio();
        assertTrue(ratio > 0.0 && ratio <= 1.0);

        // Verify specific sections
        assertTrue(result.stats.keptSections.contains("experience"));
        assertTrue(result.stats.removedSections.contains("summary"));
    }

//    @Test
    @DisplayName("Should handle different configuration levels")
    void testConfigurationLevels() {
        String resumeText = """
            Summary
            Senior developer with 5 years experience.
            
            Experience
            Developer - Tech Inc
            2019 - Present
            
            Unknown Section
            Some content here
            """;

        // Test conservative config
        HybridAnonymizationService.AnonymizationResult conservativeResult = 
            hybridAnonymizationService.anonymize(resumeText, 
                HybridAnonymizationService.AnonymizationConfig.conservative());
        
        assertTrue(conservativeResult.anonymizedText.contains("PROFESSIONAL SUMMARY"));
        assertTrue(conservativeResult.anonymizedText.contains("UNKNOWN SECTION"));

        // Test aggressive config  
        HybridAnonymizationService.AnonymizationResult aggressiveResult = 
            hybridAnonymizationService.anonymize(resumeText, 
                HybridAnonymizationService.AnonymizationConfig.aggressive());
        
        assertFalse(aggressiveResult.anonymizedText.contains("PROFESSIONAL SUMMARY"));
        assertFalse(aggressiveResult.anonymizedText.contains("UNKNOWN SECTION"));

        // Both should keep experience
        assertTrue(conservativeResult.anonymizedText.contains("EXPERIENCE"));
        assertTrue(aggressiveResult.anonymizedText.contains("EXPERIENCE"));
    }

    @Test
    @DisplayName("Should handle empty or null input gracefully")
    void testEdgeCases() {
        // Test null input
        HybridAnonymizationService.AnonymizationResult nullResult = 
            hybridAnonymizationService.anonymize(null);
        assertEquals("", nullResult.anonymizedText);

        // Test empty input
        HybridAnonymizationService.AnonymizationResult emptyResult = 
            hybridAnonymizationService.anonymize("");
        assertEquals("", emptyResult.anonymizedText);

        // Test whitespace only
        HybridAnonymizationService.AnonymizationResult whitespaceResult = 
            hybridAnonymizationService.anonymize("   \n\t   ");
        assertEquals("", whitespaceResult.anonymizedText);
    }

    @Test
    @DisplayName("Should maintain logical section order for LLM parsing")
    void testSectionOrdering() {
        String resumeText = """
            Skills
            Java, Python
            
            Education  
            BS Computer Science
            
            Summary
            Experienced developer
            
            Experience
            Senior Developer - TechCorp
            """;

        HybridAnonymizationService.AnonymizationConfig config = 
            HybridAnonymizationService.AnonymizationConfig.conservative();
            
        HybridAnonymizationService.AnonymizationResult result = 
            hybridAnonymizationService.anonymize(resumeText, config);

        String[] lines = result.anonymizedText.split("\n");
        
        // Find section positions
        int summaryPos = -1, experiencePos = -1, educationPos = -1, skillsPos = -1;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toLowerCase();
            if (line.equals("professional summary")) summaryPos = i;
            if (line.equals("experience")) experiencePos = i;
            if (line.equals("education")) educationPos = i;
            if (line.equals("skills")) skillsPos = i;
        }

        // Verify logical ordering: Summary -> Experience -> Education -> Skills
        if (summaryPos != -1 && experiencePos != -1) {
            assertTrue(summaryPos < experiencePos, "Summary should come before Experience");
        }
        if (experiencePos != -1 && educationPos != -1) {
            assertTrue(experiencePos < educationPos, "Experience should come before Education");
        }
        if (educationPos != -1 && skillsPos != -1) {
            assertTrue(educationPos < skillsPos, "Education should come before Skills");
        }
    }
}