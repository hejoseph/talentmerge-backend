package com.talentmerge.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hybrid anonymization service that combines section-based removal with light anonymization.
 * Primary strategy: Remove personal sections entirely, keep professional sections clean.
 * Perfect for LLM parsing as it provides clean professional content without confusing placeholders.
 */
@Service
public class HybridAnonymizationService {

    private final SectionSplittingService sectionSplittingService;
    private final PersonalInfoDetectionService personalInfoDetectionService;

    // Sections to keep (professional content)
    private static final Set<String> PROFESSIONAL_SECTIONS = Set.of(
            "experience", "education", "skills", "certifications", 
            "projects", "achievements", "publications", "awards"
    );

    // Sections to remove (personal information)
    private static final Set<String> PERSONAL_SECTIONS = Set.of(
            "summary", "profile", "objective", "about", "contact", 
            "personal", "interests", "hobbies", "references"
    );

    // Patterns for light anonymization within professional sections
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?[0-9][0-9 ().-]{7,20})");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("(https?://)?(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-]+/?");

    public HybridAnonymizationService(SectionSplittingService sectionSplittingService, 
                                     PersonalInfoDetectionService personalInfoDetectionService) {
        this.sectionSplittingService = sectionSplittingService;
        this.personalInfoDetectionService = personalInfoDetectionService;
    }

    /**
     * Main hybrid anonymization method
     * @param resumeText The original resume text
     * @param config Anonymization configuration
     * @return Clean professional content ready for LLM parsing
     */
    public AnonymizationResult anonymize(String resumeText, AnonymizationConfig config) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return new AnonymizationResult("", new AnonymizationStats());
        }

        AnonymizationStats stats = new AnonymizationStats();
        
        // Step 1: Split resume into sections
        Map<String, String> sections = sectionSplittingService.splitTextIntoSections(resumeText);
        stats.originalSections = sections.keySet();
        
        // WORKAROUND: If section splitting failed (only one section with all content), 
        // fall back to simple text-based section detection
        if (sections.size() == 1 && sections.containsKey("summary")) {
            String allContent = sections.get("summary");
            if (allContent.length() > 200) { // Likely failed detection
                sections = fallbackSectionDetection(allContent, stats);
                stats.originalSections = sections.keySet();
            }
        }
        
        // Step 2: Filter sections (keep professional, remove personal)
        Map<String, String> filteredSections = filterSections(sections, config, stats);
        
        // Step 3: Apply light anonymization to kept sections
        Map<String, String> cleanedSections = applylightAnonymization(filteredSections, config, stats);
        
        // Step 4: Handle professional summary (if requested and safe)
        if (config.includeCleanedSummary && sections.containsKey("summary")) {
            String cleanedSummary = extractProfessionalSummary(sections.get("summary"), stats);
            if (!cleanedSummary.trim().isEmpty()) {
                cleanedSections.put("professional_summary", cleanedSummary);
            }
        }
        
        // Step 5: Reconstruct clean resume
        String anonymizedResume = reconstructResume(cleanedSections, config);
        
        return new AnonymizationResult(anonymizedResume, stats);
    }

    /**
     * Default anonymization with standard configuration
     */
    public AnonymizationResult anonymize(String resumeText) {
        return anonymize(resumeText, AnonymizationConfig.standard());
    }

    /**
     * Fallback section detection when the main SectionSplittingService fails
     * Uses simple keyword-based detection to split content
     */
    private Map<String, String> fallbackSectionDetection(String text, AnonymizationStats stats) {
        Map<String, String> sections = new HashMap<>();
        
        // Simple patterns for common section headers
        String[] sectionPatterns = {
            "(?i)(work\\s+experience|experience|professional\\s+experience)",
            "(?i)(education|academic\\s+background|formation)",
            "(?i)(skills|technical\\s+skills|comp[eé]tences)",
            "(?i)(summary|profile|profil|objective|about)"
        };
        
        String[] sectionNames = {"experience", "education", "skills", "summary"};
        
        String[] lines = text.split("\\r?\\n");
        StringBuilder currentSection = new StringBuilder();
        String currentSectionName = "summary"; // Default section
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Check if this line is a section header
            boolean isSectionHeader = false;
            for (int i = 0; i < sectionPatterns.length; i++) {
                if (trimmedLine.matches(sectionPatterns[i])) {
                    // Save previous section if it has content
                    if (currentSection.length() > 0) {
                        sections.put(currentSectionName, currentSection.toString().trim());
                    }
                    
                    // Start new section
                    currentSectionName = sectionNames[i];
                    currentSection = new StringBuilder();
                    isSectionHeader = true;
                    break;
                }
            }
            
            // If not a section header, add to current section
            if (!isSectionHeader && !trimmedLine.isEmpty()) {
                if (currentSection.length() > 0) {
                    currentSection.append("\n");
                }
                currentSection.append(trimmedLine);
            }
        }
        
        // Save the last section
        if (currentSection.length() > 0) {
            sections.put(currentSectionName, currentSection.toString().trim());
        }
        
        // If we only found one section, try to extract at least the professional content
        if (sections.size() <= 1) {
            sections = extractProfessionalContent(text, stats);
        }
        
        stats.anonymizedItems.add("FALLBACK: Used simple section detection");
        return sections;
    }
    
    /**
     * Last resort: extract any professional content from unstructured text
     */
    private Map<String, String> extractProfessionalContent(String text, AnonymizationStats stats) {
        Map<String, String> sections = new HashMap<>();
        
        // Look for professional keywords and extract surrounding context
        // First, convert literal \n to actual line breaks
        String normalizedText = text.replace("\\n", "\n");
        String[] lines = normalizedText.split("\\r?\\n");
        StringBuilder professionalContent = new StringBuilder();
        
        for (String line : lines) {
            String lower = line.toLowerCase().trim();
            
            // Keep lines that seem professional
            if (isProfessionalLine(lower)) {
                if (professionalContent.length() > 0) {
                    professionalContent.append("\n");
                }
                professionalContent.append(line.trim());
            }
        }
        
        if (professionalContent.length() > 0) {
            sections.put("experience", professionalContent.toString());
        } else {
            // If we can't find any professional content, return empty - we prioritize privacy
            sections.put("experience", "No professional content found after anonymization.");
            stats.anonymizedItems.add("FALLBACK: No professional content detected");
        }
        
        stats.anonymizedItems.add("FALLBACK: Extracted professional content only");
        return sections;
    }
    
    /**
     * Determine if a line contains professional content
     */
    private boolean isProfessionalLine(String line) {
        if (line.length() < 5) return false;
        
        String lower = line.toLowerCase().trim();
        
        // REJECT personal information lines
        if (lower.matches(".*@.*") || // emails
            lower.matches(".*\\+?[0-9][0-9 ()-]{8,}.*") || // phones (8+ digits to avoid false positives)
            lower.matches(".*linkedin.*") || // social media
            lower.matches(".*years old.*") || lower.matches(".*born.*") ||
            lower.matches(".*live in.*") || lower.matches(".*based in.*") ||
            lower.matches(".*living in.*") || 
            lower.contains("love hiking") || lower.contains("love playing") ||
            lower.contains("free time") || lower.contains("hobbies") ||
            lower.contains("guitar") || lower.contains("photography") ||
            lower.matches("^[a-zA-Z]+\\s+[a-zA-Z]+$")) { // Simple first/last name pattern
            return false;
        }
        
        // REJECT section headers (we want content, not headers)
        if (lower.matches("^(summary|profile|experience|education|skills|about)$")) {
            return false;
        }
        
        // ACCEPT specific professional content
        boolean isProfessional = 
            // Job titles and roles
            lower.contains("engineer") || lower.contains("developer") || 
            lower.contains("manager") || lower.contains("analyst") ||
            lower.contains("director") || lower.contains("consultant") ||
            lower.contains("senior") || lower.contains("lead") ||
            // Work experience indicators
            lower.matches(".*\\d{4}\\s*[-–]\\s*\\d{4}.*") || // date ranges
            lower.matches(".*\\d{4}\\s*[-–]\\s*(present|current).*") || // employment periods
            lower.contains("developed") || lower.contains("led") ||
            lower.contains("managed") || lower.contains("implemented") ||
            lower.contains("microservices") || lower.contains("team") ||
            // Education content
            lower.contains("university") || lower.contains("college") ||
            lower.contains("degree") || lower.contains("bachelor") ||
            lower.contains("master") || lower.contains("phd") ||
            lower.contains("gpa") || lower.contains("mit") ||
            lower.contains("computer science") ||
            // Technical skills
            lower.contains("java") || lower.contains("python") ||
            lower.contains("javascript") || lower.contains("react") ||
            lower.contains("spring") || lower.contains("aws") ||
            lower.contains("docker") || lower.contains("sql") ||
            // Company names and context (keep company context)
            lower.contains("corp") || lower.contains("inc") ||
            lower.contains("ltd") || lower.contains("llc") ||
            lower.contains("tech corp") ||
            // Professional activities with years/experience
            (lower.contains("experience") && lower.matches(".*\\d+.*years.*")) ||
            // French equivalents
            lower.contains("ingénieur") || lower.contains("développeur") ||
            lower.contains("université") || lower.contains("diplôme") ||
            lower.contains("sarl") || lower.contains("sas");
            
        return isProfessional;
    }

    /**
     * Filter sections based on professional/personal classification
     */
    private Map<String, String> filterSections(Map<String, String> sections, 
                                              AnonymizationConfig config, 
                                              AnonymizationStats stats) {
        Map<String, String> filtered = new HashMap<>();
        
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String sectionKey = entry.getKey().toLowerCase();
            String sectionContent = entry.getValue();
            
            if (shouldKeepSection(sectionKey, config)) {
                filtered.put(entry.getKey(), sectionContent);
                stats.keptSections.add(entry.getKey());
            } else {
                stats.removedSections.add(entry.getKey());
                stats.removedCharacterCount += sectionContent.length();
            }
        }
        
        return filtered;
    }

    /**
     * Determine if a section should be kept based on configuration and content analysis
     */
    private boolean shouldKeepSection(String sectionKey, AnonymizationConfig config) {
        // Always keep professional sections
        if (PROFESSIONAL_SECTIONS.contains(sectionKey)) {
            return true;
        }
        
        // Always remove personal sections
        if (PERSONAL_SECTIONS.contains(sectionKey)) {
            return false;
        }
        
        // For unknown sections, apply configuration rules
        return config.keepUnknownSections;
    }

    /**
     * Apply light anonymization to professional sections
     * Remove leaked personal info while preserving professional content
     */
    private Map<String, String> applylightAnonymization(Map<String, String> sections, 
                                                       AnonymizationConfig config, 
                                                       AnonymizationStats stats) {
        Map<String, String> cleaned = new HashMap<>();
        
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String sectionContent = entry.getValue();
            String cleanedContent = cleanSectionContent(sectionContent, config, stats);
            cleaned.put(entry.getKey(), cleanedContent);
        }
        
        return cleaned;
    }

    /**
     * Clean individual section content of leaked personal information
     */
    private String cleanSectionContent(String content, AnonymizationConfig config, AnonymizationStats stats) {
        String cleaned = content;
        
        // Remove emails that leaked into professional sections
        if (config.removeLeakedEmails) {
            Matcher emailMatcher = EMAIL_PATTERN.matcher(cleaned);
            while (emailMatcher.find()) {
                stats.anonymizedItems.add("EMAIL: " + emailMatcher.group());
                cleaned = emailMatcher.replaceFirst("[EMAIL_REMOVED]");
                emailMatcher = EMAIL_PATTERN.matcher(cleaned);
            }
        }
        
        // Remove phone numbers that leaked into professional sections
        if (config.removeLeakedPhones) {
            Matcher phoneMatcher = PHONE_PATTERN.matcher(cleaned);
            while (phoneMatcher.find()) {
                String phone = phoneMatcher.group().trim();
                if (phone.length() >= 7) { // Avoid false positives with short numbers
                    stats.anonymizedItems.add("PHONE: " + phone);
                    cleaned = phoneMatcher.replaceFirst("[PHONE_REMOVED]");
                    phoneMatcher = PHONE_PATTERN.matcher(cleaned);
                }
            }
        }
        
        // Remove LinkedIn URLs that leaked into professional sections
        if (config.removeLeakedSocialMedia) {
            Matcher linkedinMatcher = LINKEDIN_PATTERN.matcher(cleaned);
            while (linkedinMatcher.find()) {
                stats.anonymizedItems.add("LINKEDIN: " + linkedinMatcher.group());
                cleaned = linkedinMatcher.replaceFirst("[LINKEDIN_REMOVED]");
                linkedinMatcher = LINKEDIN_PATTERN.matcher(cleaned);
            }
        }
        
        return cleaned;
    }

    /**
     * Extract professional content from summary while removing personal details
     */
    private String extractProfessionalSummary(String summaryContent, AnonymizationStats stats) {
        if (summaryContent == null || summaryContent.trim().isEmpty()) {
            return "";
        }
        
        String[] sentences = summaryContent.split("[.!?]+");
        StringBuilder professionalSummary = new StringBuilder();
        
        for (String sentence : sentences) {
            if (isProfessionalSentence(sentence.trim())) {
                if (professionalSummary.length() > 0) {
                    professionalSummary.append(". ");
                }
                professionalSummary.append(sentence.trim());
            } else {
                stats.removedSummaryElements.add(sentence.trim());
            }
        }
        
        return professionalSummary.toString();
    }

    /**
     * Determine if a sentence contains professional content vs personal content
     */
    private boolean isProfessionalSentence(String sentence) {
        if (sentence.length() < 10) return false;
        
        String lower = sentence.toLowerCase();
        
        // Skip sentences with personal indicators
        if (lower.contains("years old") || lower.contains("born") || lower.contains("married") ||
            lower.contains("live in") || lower.contains("based in") || lower.contains("from") ||
            lower.contains("@") || lower.matches(".*\\+?[0-9][0-9 ()-]{6,}.*")) {
            return false;
        }
        
        // Keep sentences with professional indicators
        return lower.contains("experience") || lower.contains("skilled") || lower.contains("expertise") ||
               lower.contains("developer") || lower.contains("engineer") || lower.contains("manager") ||
               lower.contains("professional") || lower.contains("specializ") || lower.contains("focus");
    }

    /**
     * Reconstruct the resume from cleaned sections
     */
    private String reconstructResume(Map<String, String> cleanedSections, AnonymizationConfig config) {
        StringBuilder result = new StringBuilder();
        
        // Preferred section order for LLM parsing
        String[] preferredOrder = {"professional_summary", "experience", "education", "skills", 
                                 "certifications", "projects", "achievements", "awards", "publications"};
        
        for (String sectionName : preferredOrder) {
            if (cleanedSections.containsKey(sectionName)) {
                String content = cleanedSections.get(sectionName).trim();
                if (!content.isEmpty()) {
                    result.append(formatSectionHeader(sectionName)).append("\n");
                    result.append(content).append("\n\n");
                }
            }
        }
        
        // Add any remaining sections not in preferred order
        for (Map.Entry<String, String> entry : cleanedSections.entrySet()) {
            if (!Arrays.asList(preferredOrder).contains(entry.getKey())) {
                String content = entry.getValue().trim();
                if (!content.isEmpty()) {
                    result.append(formatSectionHeader(entry.getKey())).append("\n");
                    result.append(content).append("\n\n");
                }
            }
        }
        
        return result.toString().trim();
    }

    /**
     * Format section headers consistently
     */
    private String formatSectionHeader(String sectionName) {
        return sectionName.toUpperCase().replace("_", " ");
    }

    /**
     * Configuration class for anonymization behavior
     */
    public static class AnonymizationConfig {
        public boolean includeCleanedSummary = false;
        public boolean removeLeakedEmails = true;
        public boolean removeLeakedPhones = true;
        public boolean removeLeakedSocialMedia = true;
        public boolean keepUnknownSections = false;
        
        public static AnonymizationConfig standard() {
            return new AnonymizationConfig();
        }
        
        public static AnonymizationConfig conservative() {
            AnonymizationConfig config = new AnonymizationConfig();
            config.includeCleanedSummary = true;
            config.keepUnknownSections = true;
            return config;
        }
        
        public static AnonymizationConfig aggressive() {
            AnonymizationConfig config = new AnonymizationConfig();
            config.includeCleanedSummary = false;
            config.keepUnknownSections = false;
            return config;
        }
    }

    /**
     * Statistics about the anonymization process
     */
    public static class AnonymizationStats {
        public Set<String> originalSections = new HashSet<>();
        public Set<String> keptSections = new HashSet<>();
        public Set<String> removedSections = new HashSet<>();
        public List<String> anonymizedItems = new ArrayList<>();
        public List<String> removedSummaryElements = new ArrayList<>();
        public int removedCharacterCount = 0;
        
        public double getAnonymizationRatio() {
            if (originalSections.isEmpty()) return 0.0;
            return (double) removedSections.size() / originalSections.size();
        }
    }

    /**
     * Result of anonymization process
     */
    public static class AnonymizationResult {
        public final String anonymizedText;
        public final AnonymizationStats stats;
        
        public AnonymizationResult(String anonymizedText, AnonymizationStats stats) {
            this.anonymizedText = anonymizedText;
            this.stats = stats;
        }
    }
}