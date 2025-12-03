package com.talentmerge.service;

import com.talentmerge.model.WorkExperience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for enhanced work experience parsing
 */
class WorkExperienceParsingServiceTest {

    private WorkExperienceParsingService workExperienceParsingService;

    @BeforeEach
    void setUp() {
        workExperienceParsingService = new WorkExperienceParsingService();
    }

    @Test
    @DisplayName("Test Standard English Format")
    void testStandardEnglishFormat() {
        String experienceText = """
            Senior Software Engineer
            Google Inc.
            January 2020 - Present
            • Developed scalable microservices
            
            Software Developer
            Microsoft Corporation
            June 2018 - December 2019
            • Built cloud solutions
            """;

        List<WorkExperience> experiences = workExperienceParsingService.parseWorkExperience(experienceText);

        assertEquals(2, experiences.size());
        
        WorkExperience exp1 = experiences.get(0);
        assertEquals("Senior Software Engineer", exp1.getJobTitle());
        assertEquals("Google Inc.", exp1.getCompany());
        assertEquals(LocalDate.of(2020, 1, 1), exp1.getStartDate());
        assertNull(exp1.getEndDate());
    }

    @Test
    @DisplayName("Test French Format")
    void testFrenchFormat() {
        String experienceText = """
            Ingénieur Logiciel Senior
            Google France
            janvier 2020 - Aujourd'hui
            • Développement de microservices
            """;

        List<WorkExperience> experiences = workExperienceParsingService.parseWorkExperience(experienceText);

        assertTrue(experiences.size() >= 1);
        WorkExperience exp = experiences.get(0);
        assertNotNull(exp.getJobTitle());
        assertNotNull(exp.getCompany());
    }

    @Test
    @DisplayName("Test Edge Cases")
    void testEdgeCases() {
        List<WorkExperience> nullResult = workExperienceParsingService.parseWorkExperience(null);
        assertEquals(0, nullResult.size());

        List<WorkExperience> emptyResult = workExperienceParsingService.parseWorkExperience("");
        assertEquals(0, emptyResult.size());
    }
}