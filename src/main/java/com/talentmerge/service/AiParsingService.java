package com.talentmerge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.talentmerge.model.Candidate;
import com.talentmerge.model.Education;
import com.talentmerge.model.WorkExperience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Qualifier("ai")
public class AiParsingService implements IParsingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiParsingService.class);
    private final ChatModel chatModel;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    public AiParsingService(ChatModel chatModel, PromptService promptService) {
        this.chatModel = chatModel;
        this.promptService = promptService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Candidate parseCandidateFromText(String text) {
        try {
            logger.info("Starting AI parsing for resume text of length: {}", text.length());
            
            // Create prompt for resume parsing
            String prompt = promptService.createResumeParsingPrompt(text);
            
            // Call OpenRouter via Spring AI
            logger.debug("Sending request to OpenRouter...");
            String aiResponse = chatModel.call(prompt);
            logger.debug("Received response from OpenRouter: {}", aiResponse.substring(0, Math.min(200, aiResponse.length())));
            
            // Parse the JSON response
            return parseAiResponseToCandidate(aiResponse);
            
        } catch (Exception e) {
            logger.error("Error during AI parsing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse resume with AI: " + e.getMessage(), e);
        }
    }
    
    public Candidate parseAnonymizedCandidateFromText(String anonymizedText) {
        try {
            logger.info("Starting AI parsing for anonymized resume text of length: {}", anonymizedText.length());
            
            // Create prompt for anonymized resume parsing
            String prompt = promptService.createAnonymizedResumeParsingPrompt(anonymizedText);
            
            // Call OpenRouter via Spring AI
            logger.debug("Sending anonymized resume request to OpenRouter...");
            String aiResponse = chatModel.call(prompt);
            logger.debug("Received response from OpenRouter for anonymized resume: {}", 
                        aiResponse.substring(0, Math.min(200, aiResponse.length())));
            
            // Parse the JSON response
            return parseAiResponseToCandidate(aiResponse);
            
        } catch (Exception e) {
            logger.error("Error during anonymized AI parsing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse anonymized resume with AI: " + e.getMessage(), e);
        }
    }

    private Candidate parseAiResponseToCandidate(String aiResponse) {
        try {
            // Clean the response in case it contains markdown or extra text
            String jsonResponse = extractJsonFromResponse(aiResponse);
            
            // Parse JSON response
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            // Create candidate object
            Candidate candidate = new Candidate();
            candidate.setName(getStringValue(rootNode, "name"));
            candidate.setEmail(getStringValue(rootNode, "email"));
            candidate.setPhone(getStringValue(rootNode, "phone"));
            candidate.setSkills(getStringValue(rootNode, "skills"));
            
            // Parse work experiences
            if (rootNode.has("workExperiences") && rootNode.get("workExperiences").isArray()) {
                List<WorkExperience> workExperiences = new ArrayList<>();
                for (JsonNode workNode : rootNode.get("workExperiences")) {
                    WorkExperience work = new WorkExperience();
                    work.setJobTitle(getStringValue(workNode, "jobTitle"));
                    work.setCompany(getStringValue(workNode, "company"));
                    work.setDescription(getStringValue(workNode, "description"));
                    work.setStartDate(parseDate(getStringValue(workNode, "startDate")));
                    work.setEndDate(parseDate(getStringValue(workNode, "endDate")));
                    candidate.addWorkExperience(work);
                }
            }
            
            // Parse educations
            if (rootNode.has("educations") && rootNode.get("educations").isArray()) {
                List<Education> educations = new ArrayList<>();
                for (JsonNode eduNode : rootNode.get("educations")) {
                    Education education = new Education();
                    education.setInstitution(getStringValue(eduNode, "institution"));
                    education.setDegree(getStringValue(eduNode, "degree"));
                    education.setGraduationDate(parseDate(getStringValue(eduNode, "graduationDate")));
                    candidate.addEducation(education);
                }
            }
            
            logger.info("Successfully parsed candidate: {} with {} work experiences and {} educations", 
                       candidate.getName(), candidate.getWorkExperiences().size(), candidate.getEducations().size());
            
            return candidate;
            
        } catch (Exception e) {
            logger.error("Error parsing AI response to candidate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
    
    private String extractJsonFromResponse(String response) {
        // Remove markdown code blocks if present
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        // Find the first { and last } to extract just the JSON
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }
        
        return cleaned.trim();
    }
    
    private String getStringValue(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            String value = node.get(fieldName).asText();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
    
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty() || "null".equals(dateString)) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse date '{}': {}", dateString, e.getMessage());
            return null;
        }
    }
}
