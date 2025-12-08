package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiParsingServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private PromptService promptService;

    private AiParsingService aiParsingService;

    @BeforeEach
    void setUp() {
        aiParsingService = new AiParsingService(chatModel, promptService);
    }

    @Test
    void testParseCandidateFromText_Success() {
        // Given
        String resumeText = "John Doe\nSoftware Engineer\njohn@email.com\n+1234567890\nJava, Python, Spring Boot";
        
        String mockPrompt = "Parse this resume: " + resumeText;
        when(promptService.createResumeParsingPrompt(resumeText)).thenReturn(mockPrompt);
        
        String mockAiResponse = """
            {
                "name": "John Doe",
                "email": "john@email.com",
                "phone": "+1234567890",
                "skills": "Java, Python, Spring Boot",
                "workExperiences": [
                    {
                        "jobTitle": "Software Engineer",
                        "company": "Tech Corp",
                        "startDate": "2020-01-01",
                        "endDate": null,
                        "description": "Developed web applications"
                    }
                ],
                "educations": [
                    {
                        "institution": "University XYZ",
                        "degree": "Bachelor of Computer Science",
                        "graduationDate": "2019-12-01"
                    }
                ]
            }
            """;
        
        when(chatModel.call(mockPrompt)).thenReturn(mockAiResponse);

        // When
        Candidate result = aiParsingService.parseCandidateFromText(resumeText);

        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@email.com", result.getEmail());
        assertEquals("+1234567890", result.getPhone());
        assertEquals("Java, Python, Spring Boot", result.getSkills());
        assertEquals(1, result.getWorkExperiences().size());
        assertEquals(1, result.getEducations().size());
    }

    @Test
    void testParseCandidateFromText_WithMarkdownResponse() {
        // Given
        String resumeText = "Simple resume text";
        String mockPrompt = "Parse resume";
        when(promptService.createResumeParsingPrompt(resumeText)).thenReturn(mockPrompt);
        
        String mockAiResponseWithMarkdown = """
            ```json
            {
                "name": "Jane Smith",
                "email": null,
                "phone": null,
                "skills": "JavaScript, React",
                "workExperiences": [],
                "educations": []
            }
            ```
            """;
        
        when(chatModel.call(mockPrompt)).thenReturn(mockAiResponseWithMarkdown);

        // When
        Candidate result = aiParsingService.parseCandidateFromText(resumeText);

        // Then
        assertNotNull(result);
        assertEquals("Jane Smith", result.getName());
        assertNull(result.getEmail());
        assertNull(result.getPhone());
        assertEquals("JavaScript, React", result.getSkills());
        assertTrue(result.getWorkExperiences().isEmpty());
        assertTrue(result.getEducations().isEmpty());
    }

    @Test
    void testParseCandidateFromText_ChatModelFailure() {
        // Given
        String resumeText = "Some resume text";
        String mockPrompt = "Parse resume";
        when(promptService.createResumeParsingPrompt(resumeText)).thenReturn(mockPrompt);
        when(chatModel.call(mockPrompt)).thenThrow(new RuntimeException("API Error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> aiParsingService.parseCandidateFromText(resumeText));
        
        assertTrue(exception.getMessage().contains("Failed to parse resume with AI"));
    }

    @Test
    void testParseCandidateFromText_InvalidJsonResponse() {
        // Given
        String resumeText = "Some resume text";
        String mockPrompt = "Parse resume";
        when(promptService.createResumeParsingPrompt(resumeText)).thenReturn(mockPrompt);
        when(chatModel.call(mockPrompt)).thenReturn("Invalid JSON response");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> aiParsingService.parseCandidateFromText(resumeText));
        
        assertTrue(exception.getMessage().contains("Failed to parse resume with AI"));
    }
}