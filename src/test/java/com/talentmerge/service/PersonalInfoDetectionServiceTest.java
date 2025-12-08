package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersonalInfoDetectionServiceTest {

    private PersonalInfoDetectionService personalInfoDetectionService;

    @BeforeEach
    void setUp() {
        personalInfoDetectionService = new PersonalInfoDetectionService();
    }

    @Test
    @DisplayName("Should extract personal info from resume text")
    void testDetectPersonalInfo() {
        String resumeText = "John Doe\n" +
                            "Software Engineer\n" +
                            "john.doe@example.com\n" +
                            "+1 123 456 7890\n" +
                            "linkedin.com/in/johndoe";

        Candidate candidate = personalInfoDetectionService.detectPersonalInfo(resumeText);

        assertEquals("John Doe", candidate.getName());
        assertEquals("john.doe@example.com", candidate.getEmail());
        assertEquals("+1 123 456 7890", candidate.getPhone());
    }

    @Test
    @DisplayName("Should extract LinkedIn URL")
    void testExtractLinkedInUrl() {
        String text = "My profile is on linkedin.com/in/johndoe";
        String expected = "https://www.linkedin.com/in/johndoe";
        String actual = personalInfoDetectionService.extractLinkedInUrl(text);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Should return null if no LinkedIn URL is found")
    void testExtractLinkedInUrl_NotFound() {
        String text = "My profile is not on LinkedIn";
        String actual = personalInfoDetectionService.extractLinkedInUrl(text);
        assertNull(actual);
    }
}
