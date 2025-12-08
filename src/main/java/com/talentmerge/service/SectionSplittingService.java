package com.talentmerge.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;

/**
 * Dedicated service for splitting resume text into logical sections
 * Handles multi-line headers, various formatting patterns, and language-specific conventions
 */
@Service
public class SectionSplittingService {

    private static final List<String> SECTION_KEYWORDS = Arrays.asList(
            // English
            "experience", "employment history", "work experience", "professional experience",
            "work history", "career history", "employment",
            "education", "academic background", "academic history",
            "skills", "technical skills", "competencies", "core competencies",
            "summary", "profile", "objective", "about",
            // French
            "expérience professionnelle", "expériences professionnelles", "expériences",
            "expérience", "historique professionnel", "parcours professionnel",
            "formation", "formations", "éducation", "parcours académique",
            "compétences", "compétences techniques", "savoir-faire",
            "profil", "à propos", "résumé", "objectif"
    );

    public Map<String, String> splitTextIntoSections(String resumeText) {
        Map<String, String> sections = new HashMap<>();
        
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return sections;
        }
        
        // Split text into lines
        String[] lines = resumeText.split("\\n");
        List<SectionHeader> sectionHeaders = findSectionHeaders(lines);
        
        // Extract content for each section
        for (int i = 0; i < sectionHeaders.size(); i++) {
            SectionHeader currentHeader = sectionHeaders.get(i);
            int startLine = currentHeader.endLine + 1;
            int endLine = (i + 1 < sectionHeaders.size()) ? 
                          sectionHeaders.get(i + 1).startLine - 1 : 
                          lines.length - 1;
            
            StringBuilder content = new StringBuilder();
            for (int j = startLine; j <= endLine; j++) {
                if (j < lines.length && !lines[j].trim().isEmpty()) {
                    if (content.length() > 0) {
                        content.append("\n");
                    }
                    content.append(lines[j].trim());
                }
            }
            
            String sectionContent = content.toString().trim();
            if (!sectionContent.isEmpty()) {
                sections.put(currentHeader.standardKey, sectionContent);
            }
        }
        
        return sections;
    }
    
    private List<SectionHeader> findSectionHeaders(String[] lines) {
        List<SectionHeader> headers = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            // Check for multi-line headers first (look ahead)
            boolean foundMultiLineHeader = false;
            if (i + 1 < lines.length) {
                String nextLine = lines[i + 1].trim();
                if (!nextLine.isEmpty()) {
                    String combinedHeader = line + " " + nextLine;
                    String standardKey = getStandardSectionKey(combinedHeader);
                    if (standardKey != null) {
                        headers.add(new SectionHeader(standardKey, i, i + 1));
                        i++; // Skip the next line as it's part of this header
                        foundMultiLineHeader = true;
                    }
                }
            }
            
            // Check for single-line headers only if no multi-line header was found
            if (!foundMultiLineHeader) {
                String standardKey = getStandardSectionKey(line);
                if (standardKey != null) {
                    headers.add(new SectionHeader(standardKey, i, i));
                }
            }
        }
        
        return headers;
    }
    
    private String getStandardSectionKey(String text) {
        String normalizedText = normalizeText(text.toLowerCase());
        
        // Summary variants - check first to avoid conflicts
        if (normalizedText.equals("summary") || normalizedText.equals("profile") || 
            normalizedText.equals("objective") || normalizedText.equals("about") || 
            normalizedText.equals("resume") || normalizedText.equals("profil")) {
            return "summary";
        }
        
        // Experience variants
        if (normalizedText.equals("experience") || normalizedText.equals("work experience") || 
            normalizedText.equals("professional experience") || normalizedText.equals("employment history") ||
            normalizedText.equals("work history") || normalizedText.equals("career history") ||
            normalizedText.equals("employment") || normalizedText.equals("experience professionnelle") ||
            normalizedText.equals("experiences professionnelles") || normalizedText.equals("experiences") ||
            normalizedText.equals("historique professionnel") || normalizedText.equals("parcours professionnel")) {
            return "experience";
        }
        
        // Education variants
        if (normalizedText.equals("education") || normalizedText.equals("academic background") || 
            normalizedText.equals("academic history") || normalizedText.equals("formation") ||
            normalizedText.equals("formations") || normalizedText.equals("parcours academique") ||
            normalizedText.equals("education et formation")) {
            return "education";
        }
        
        // Skills variants
        if (normalizedText.equals("skills") || normalizedText.equals("technical skills") || 
            normalizedText.equals("competencies") || normalizedText.equals("core competencies") ||
            normalizedText.equals("competences") || normalizedText.equals("competences techniques") ||
            normalizedText.equals("savoir-faire")) {
            return "skills";
        }
        
        return null;
    }
    
    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
    
    private String normalizeText(String text) {
        // Remove accents and normalize Unicode
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", ""); // Remove diacritical marks
    }
    
    private static class SectionHeader {
        String standardKey;
        int startLine;
        int endLine;
        
        SectionHeader(String standardKey, int startLine, int endLine) {
            this.standardKey = standardKey;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }
}
