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

    /**
     * Main method to split text into sections
     */
    public Map<String, String> splitTextIntoSections(String text) {
        // Stage 1: Text Preprocessing
        String preprocessedText = preprocessText(text);

        Map<String, String> sections = new HashMap<>();
        String[] lines = preprocessedText.split("\\r?\\n");

        // Stage 2: Detect header candidates
        List<HeaderCandidate> candidates = detectHeaders(lines);

        // Stage 3: Validate headers (placeholder for now)
        List<HeaderCandidate> validatedHeaders = candidates.stream()
                .sorted(Comparator.comparingInt(a -> a.startLineIndex))
                .toList();

        // Stage 4: Extract section content
        return extractSections(validatedHeaders, lines);
    }

    /**
     * Stage 1: Preprocess text to normalize formatting and fix common issues
     */
    private String preprocessText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 1. Unicode normalization - decompose accented characters then recompose
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);

        // 2. Fix common OCR errors
        normalized = fixCommonOcrErrors(normalized);

        // 3. Standardize line breaks
        normalized = normalized.replaceAll("\\r\\n", "\n")
                              .replaceAll("\\r", "\n");

        // 4. Clean up excessive whitespace while preserving structure
        normalized = cleanWhitespace(normalized);

        return normalized;
    }

    /**
     * Fix common OCR errors that affect section detection
     */
    private String fixCommonOcrErrors(String text) {
        return text
            // Fix common character misrecognition
            .replaceAll("\\bl\\b", "I")  // lowercase l mistaken for I
            .replaceAll("\\brn\\b", "m") // rn mistaken for m
            .replaceAll("\\bO\\b", "0")  // O mistaken for 0 in dates
            // Fix common French accent issues
            .replaceAll("e'", "é")
            .replaceAll("a'", "à")
            .replaceAll("E'", "É")
            .replaceAll("A'", "À")
            // Fix spacing around punctuation
            .replaceAll("\\s+:", ":")
            .replaceAll("\\s+;", ";");
    }

    /**
     * Clean whitespace while preserving document structure
     */
    private String cleanWhitespace(String text) {
        String[] lines = text.split("\\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            // Remove trailing whitespace
            String trimmed = line.replaceAll("\\s+$", "");

            // Normalize internal whitespace (multiple spaces/tabs to single space)
            trimmed = trimmed.replaceAll("\\s+", " ");

            // Preserve leading whitespace structure for indentation
            if (!trimmed.trim().isEmpty()) {
                cleaned.append(trimmed).append("\n");
            } else if (cleaned.length() > 0 && cleaned.charAt(cleaned.length() - 1) != '\n') {
                // Preserve single empty lines for structure
                cleaned.append("\n");
            }
        }

        return cleaned.toString();
    }

    /**
     * Stage 2: Detect potential section headers with multi-line support
     */
    private List<HeaderCandidate> detectHeaders(String[] lines) {
//        List<HeaderCADMINate> candidates = new ArrayList<>();
//
//        for (int i = 0; i < lines.length; i++) {
//            String currentLine = lines[i].trim();
//            if (currentLine.isEmpty()) continue;
//
//            // Check single-line headers
//            HeaderCandidate singleLine = checkSingleLineHeader(currentLine, i);
//            if (singleLine != null) {
//                candidates.add(singleLine);
//            }
//        }

        return null;
    }

    /**
     * Check for single-line section headers
     */
    private HeaderCandidate checkSingleLineHeader(String line, int lineIndex) {
        String lowerLine = line.toLowerCase();

        for (String keyword : SECTION_KEYWORDS) {
            String lowerKeyword = keyword.toLowerCase();

            if (lowerLine.equals(lowerKeyword) ||
                (line.toUpperCase().equals(line) && lowerLine.contains(lowerKeyword) && line.length() < 50)) {

                double confidence = calculateSingleLineConfidence(line, keyword);
                if (confidence > 0.6) {
                    return new HeaderCandidate(line, lineIndex, lineIndex, HeaderType.SINGLE_LINE, confidence, keyword);
                }
            }
        }

        return null;
    }

    /**
     * Check for multi-line section headers
     */
    private HeaderCandidate checkMultiLineHeader(String[] lines, int startIndex) {
        if (startIndex >= lines.length - 1) return null;

        String firstLine = lines[startIndex].trim();
        String secondLine = startIndex + 1 < lines.length ? lines[startIndex + 1].trim() : "";
        String thirdLine = startIndex + 2 < lines.length ? lines[startIndex + 2].trim() : "";

        // Try 2-line combinations
        String twoLineCombo = (firstLine + " " + secondLine).toLowerCase();
        HeaderCandidate twoLineHeader = checkMultiLineCombo(twoLineCombo, firstLine, secondLine, startIndex, startIndex + 1);
        if (twoLineHeader != null) return twoLineHeader;

        // Try 3-line combinations (rare but possible)
        if (!thirdLine.isEmpty() && thirdLine.length() < 30) {
            String threeLineCombo = (firstLine + " " + secondLine + " " + thirdLine).toLowerCase();
            HeaderCandidate threeLineHeader = checkMultiLineCombo(threeLineCombo, firstLine, secondLine + " " + thirdLine, startIndex, startIndex + 2);
            if (threeLineHeader != null) return threeLineHeader;
        }

        return null;
    }

    /**
     * Check if multi-line combination matches section keywords
     */
    private HeaderCandidate checkMultiLineCombo(String combo, String firstLine, String restLines, int startIndex, int endIndex) {
        for (String keyword : SECTION_KEYWORDS) {
            String lowerKeyword = keyword.toLowerCase();

            if (combo.contains(lowerKeyword)) {
                double confidence = calculateMultiLineConfidence(combo, keyword, firstLine, restLines);
                if (confidence > 0.7) {
                    String headerText = firstLine + "\n" + restLines;
                    return new HeaderCandidate(headerText, startIndex, endIndex, HeaderType.MULTI_LINE, confidence, keyword);
                }
            }
        }
        return null;
    }

    /**
     * Check for bulleted section headers (• Experience, - Skills, etc.)
     */
    private HeaderCandidate checkBulletedHeader(String line, int lineIndex) {
        if (!line.matches("^[•\\-\\*\\+]\\s+.*")) return null;

        String content = line.replaceFirst("^[•\\-\\*\\+]\\s+", "").toLowerCase();

        for (String keyword : SECTION_KEYWORDS) {
            if (content.contains(keyword.toLowerCase()) && content.length() < 50) {
                double confidence = calculateBulletedConfidence(content, keyword);
                if (confidence > 0.6) {
                    return new HeaderCandidate(line, lineIndex, lineIndex, HeaderType.BULLETED, confidence, keyword);
                }
            }
        }

        return null;
    }

    /**
     * Check for indented section headers
     */
    private HeaderCandidate checkIndentedHeader(String line, int lineIndex) {
        if (!line.matches("^\\s{3,}\\S.*")) return null; // At least 3 spaces of indentation

        String content = line.trim().toLowerCase();

        for (String keyword : SECTION_KEYWORDS) {
            if (content.contains(keyword.toLowerCase()) && content.length() < 50) {
                double confidence = calculateIndentedConfidence(content, keyword);
                if (confidence > 0.6) {
                    return new HeaderCandidate(line, lineIndex, lineIndex, HeaderType.INDENTED, confidence, keyword);
                }
            }
        }

        return null;
    }

    /**
     * Stage 3: Validate headers with context analysis
     */
    private List<HeaderCandidate> validateHeaders(List<HeaderCandidate> candidates, String[] lines) {
        List<HeaderCandidate> validated = new ArrayList<>();

        for (HeaderCandidate candidate : candidates) {
            ValidationResult result = validateHeaderContext(candidate, lines);
            if (result.isValid) {
                // Update confidence based on context validation
                candidate.confidence = Math.min(candidate.confidence * result.confidenceMultiplier, 1.0);
                validated.add(candidate);
            }
        }

        // Remove overlapping headers (keep highest confidence)
        validated = removeOverlappingHeaders(validated);

        // Sort by line position
        return validated.stream()
                .sorted((a, b) -> Integer.compare(a.startLineIndex, b.startLineIndex))
                .toList();
    }

    /**
     * Validate header context to filter false positives
     */
    private ValidationResult validateHeaderContext(HeaderCandidate candidate, String[] lines) {
        ValidationResult result = new ValidationResult();
        result.isValid = true;
        result.confidenceMultiplier = 1.0;
        result.reasons = new ArrayList<>();

        // Check 1: Avoid false positives with company names
        if (isCompanyNameFalsePositive(candidate, lines)) {
            result.isValid = false;
            result.reasons.add("Likely company name containing section keyword");
            return result;
        }

        // Check 2: Validate content after header
        double contentScore = validateSectionContent(candidate, lines);
        if (contentScore < 0.3) {
            result.isValid = false;
            result.reasons.add("Content after header doesn't match expected section type");
            return result;
        }
        result.confidenceMultiplier *= (0.7 + 0.3 * contentScore);

        // Check 3: Check surrounding context
        double contextScore = analyzeContextLines(candidate, lines);
        result.confidenceMultiplier *= (0.8 + 0.2 * contextScore);

        // Check 4: Position validation (headers shouldn't be at very end)
        if (candidate.endLineIndex >= lines.length - 2) {
            result.confidenceMultiplier *= 0.7;
            result.reasons.add("Header near end of document");
        }

        // Check 5: Length validation for multi-line headers
        if (candidate.type == HeaderType.MULTI_LINE) {
            if (candidate.text.length() > 60) {
                result.confidenceMultiplier *= 0.6;
                result.reasons.add("Multi-line header too long");
            }
        }

        // Final confidence check
        if (candidate.confidence * result.confidenceMultiplier < 0.4) {
            result.isValid = false;
            result.reasons.add("Final confidence score too low");
        }

        return result;
    }

    /**
     * Check if detected header is actually a company name with section keywords
     */
    private boolean isCompanyNameFalsePositive(HeaderCandidate candidate, String[] lines) {
        String headerText = candidate.text.toLowerCase();

        // Look for company indicators
        if (headerText.contains("inc.") || headerText.contains("corp.") ||
            headerText.contains("ltd.") || headerText.contains("llc") ||
            headerText.contains("sarl") || headerText.contains("sas") ||
            headerText.contains("gmbh") || headerText.contains("ag")) {
            return true;
        }

        // Check surrounding lines for context clues
        int startCheck = Math.max(0, candidate.startLineIndex - 2);
        int endCheck = Math.min(lines.length, candidate.endLineIndex + 3);

        for (int i = startCheck; i < endCheck; i++) {
            if (i == candidate.startLineIndex) continue;
            String line = lines[i].toLowerCase();

            // Look for date ranges (suggests this is work experience entry, not header)
            if (line.matches(".*\\d{4}\\s*[-–]\\s*\\d{4}.*") ||
                line.matches(".*\\d{4}\\s*[-–]\\s*(present|current|aujourd'hui|actuel).*")) {
                return true;
            }

            // Look for job title indicators
            if (line.contains("engineer") || line.contains("developer") ||
                line.contains("manager") || line.contains("director") ||
                line.contains("ingénieur") || line.contains("développeur") ||
                line.contains("responsable") || line.contains("directeur")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate that content after header matches expected section type
     */
    private double validateSectionContent(HeaderCandidate candidate, String[] lines) {
        int contentStart = candidate.endLineIndex + 1;
        int contentEnd = Math.min(lines.length, contentStart + 10); // Check first 10 lines

        if (contentStart >= lines.length) return 0.0;

        String sectionType = normalizeSectionKey(candidate.matchedKeyword);
        double score = 0.0;
        int validLines = 0;

        for (int i = contentStart; i < contentEnd; i++) {
            String line = lines[i].trim().toLowerCase();
            if (line.isEmpty()) continue;

            validLines++;

            switch (sectionType) {
                case "experience":
                    score += validateExperienceContent(line);
                    break;
                case "education":
                    score += validateEducationContent(line);
                    break;
                case "skills":
                    score += validateSkillsContent(line);
                    break;
                default:
                    score += 0.5; // Neutral for unknown sections
            }
        }

        return validLines > 0 ? score / validLines : 0.0;
    }

    /**
     * Validate experience section content
     */
    private double validateExperienceContent(String line) {
        double score = 0.3; // Base score

        // Look for job titles
        if (line.contains("engineer") || line.contains("developer") || line.contains("manager") ||
            line.contains("analyst") || line.contains("consultant") || line.contains("director") ||
            line.contains("ingénieur") || line.contains("développeur") || line.contains("responsable") ||
            line.contains("chef") || line.contains("directeur") || line.contains("consultant")) {
            score += 0.3;
        }

        // Look for company names or dates
        if (line.matches(".*\\d{4}.*") || line.contains("inc") || line.contains("corp") ||
            line.contains("ltd") || line.contains("sarl") || line.contains("sas")) {
            score += 0.2;
        }

        // Look for experience indicators
        if (line.contains("developed") || line.contains("managed") || line.contains("led") ||
            line.contains("implemented") || line.contains("designed") ||
            line.contains("développé") || line.contains("géré") || line.contains("dirigé") ||
            line.contains("implémenté") || line.contains("conçu")) {
            score += 0.3;
        }

        return Math.min(score, 1.0);
    }

    /**
     * Validate education section content
     */
    private double validateEducationContent(String line) {
        double score = 0.3; // Base score

        // Look for degrees
        if (line.contains("bachelor") || line.contains("master") || line.contains("phd") ||
            line.contains("diploma") || line.contains("degree") ||
            line.contains("licence") || line.contains("master") || line.contains("doctorat") ||
            line.contains("diplôme") || line.contains("bts") || line.contains("dut")) {
            score += 0.4;
        }

        // Look for universities/schools
        if (line.contains("university") || line.contains("college") || line.contains("school") ||
            line.contains("institute") || line.contains("université") || line.contains("école") ||
            line.contains("institut") || line.contains("lycée")) {
            score += 0.3;
        }

        // Look for graduation years
        if (line.matches(".*\\d{4}.*") || line.contains("graduated") || line.contains("diplômé")) {
            score += 0.2;
        }

        return Math.min(score, 1.0);
    }

    /**
     * Validate skills section content
     */
    private double validateSkillsContent(String line) {
        double score = 0.3; // Base score

        // Look for technical skills
        if (line.contains("java") || line.contains("python") || line.contains("javascript") ||
            line.contains("sql") || line.contains("aws") || line.contains("docker") ||
            line.contains("react") || line.contains("angular") || line.contains("spring")) {
            score += 0.4;
        }

        // Look for skill categories
        if (line.contains("programming") || line.contains("languages") || line.contains("frameworks") ||
            line.contains("databases") || line.contains("tools") ||
            line.contains("programmation") || line.contains("langages") || line.contains("outils")) {
            score += 0.3;
        }

        // Look for skill-like formatting (comma-separated)
        if (line.contains(",") && line.split(",").length > 2) {
            score += 0.3;
        }

        return Math.min(score, 1.0);
    }

    /**
     * Analyze context lines around the header for additional validation
     */
    private double analyzeContextLines(HeaderCandidate candidate, String[] lines) {
        double score = 0.5; // Base score

        // Check if previous lines suggest this is a real section break
        int prevStart = Math.max(0, candidate.startLineIndex - 3);
        boolean hasContentBefore = false;

        for (int i = prevStart; i < candidate.startLineIndex; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && line.length() > 10) {
                hasContentBefore = true;
                break;
            }
        }

        if (hasContentBefore) score += 0.2;

        // Check if there's meaningful content after
        int nextEnd = Math.min(lines.length, candidate.endLineIndex + 5);
        boolean hasContentAfter = false;

        for (int i = candidate.endLineIndex + 1; i < nextEnd; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && line.length() > 5) {
                hasContentAfter = true;
                break;
            }
        }

        if (hasContentAfter) score += 0.3;

        return Math.min(score, 1.0);
    }

    /**
     * Remove overlapping headers, keeping the one with highest confidence
     */
    private List<HeaderCandidate> removeOverlappingHeaders(List<HeaderCandidate> candidates) {
        List<HeaderCandidate> result = new ArrayList<>();

        for (HeaderCandidate candidate : candidates) {
            boolean overlaps = false;

            for (int i = 0; i < result.size(); i++) {
                HeaderCandidate existing = result.get(i);

                // Check if ranges overlap
                if (candidate.startLineIndex <= existing.endLineIndex &&
                    candidate.endLineIndex >= existing.startLineIndex) {
                    overlaps = true;

                    // Keep the one with higher confidence
                    if (candidate.confidence > existing.confidence) {
                        result.set(i, candidate);
                    }
                    break;
                }
            }

            if (!overlaps) {
                result.add(candidate);
            }
        }

        return result;
    }

    /**
     * Validation result class
     */
    private static class ValidationResult {
        boolean isValid;
        double confidenceMultiplier;
        List<String> reasons;

        ValidationResult() {
            this.reasons = new ArrayList<>();
        }
    }

    /**
     * Stage 4: Extract section content
     */
    private Map<String, String> extractSections(List<HeaderCandidate> headers, String[] lines) {
        Map<String, String> sections = new HashMap<>();

        if (headers.isEmpty()) {
            // If no headers found, put everything in summary
            sections.put("summary", String.join("\n", lines));
            return sections;
        }

        // Add content before first header as summary
        if (headers.get(0).startLineIndex > 0) {
            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < headers.get(0).startLineIndex; i++) {
                summary.append(lines[i]).append("\n");
            }
            sections.put("summary", summary.toString().trim());
        }

        // Extract content for each section
        for (int i = 0; i < headers.size(); i++) {
            HeaderCandidate header = headers.get(i);
            int startLine = header.endLineIndex + 1;
            int endLine = (i + 1 < headers.size()) ? headers.get(i + 1).startLineIndex : lines.length;

            StringBuilder content = new StringBuilder();
            for (int j = startLine; j < endLine; j++) {
                content.append(lines[j]).append("\n");
            }

            String sectionKey = normalizeSectionKey(header.matchedKeyword);
            sections.put(sectionKey, content.toString().trim());
        }

        return sections;
    }

    /**
     * Normalize section keys to standard values
     */
    private String normalizeSectionKey(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        if (lowerKeyword.contains("experience") || lowerKeyword.contains("expérience") ||
            lowerKeyword.contains("employment") || lowerKeyword.contains("career") ||
            lowerKeyword.contains("work") || lowerKeyword.contains("professional") ||
            lowerKeyword.contains("parcours professionnel") || lowerKeyword.contains("historique professionnel"))
            return "experience";
        if (lowerKeyword.contains("education") || lowerKeyword.contains("formation") ||
            lowerKeyword.contains("academic") || lowerKeyword.contains("éducation") ||
            lowerKeyword.contains("parcours académique"))
            return "education";
        if (lowerKeyword.contains("skill") || lowerKeyword.contains("compétences") ||
            lowerKeyword.contains("competencies") || lowerKeyword.contains("savoir-faire"))
            return "skills";
        if (lowerKeyword.contains("summary") || lowerKeyword.contains("profil") ||
            lowerKeyword.contains("à propos") || lowerKeyword.contains("objective") ||
            lowerKeyword.contains("résumé") || lowerKeyword.contains("about"))
            return "summary";
        return "summary"; // Default
    }

    // Confidence calculation methods
    private double calculateSingleLineConfidence(String line, String keyword) {
        double confidence = 0.5;
        String lowerLine = line.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();

        if (lowerLine.equals(lowerKeyword)) confidence += 0.4;
        if (line.equals(line.toUpperCase())) confidence += 0.2;
        if (line.length() < 30) confidence += 0.1;
        if (line.contains("Inc.") || line.contains("Corp.") || line.contains("Ltd.")) confidence -= 0.3;

        return Math.min(confidence, 1.0);
    }

    private double calculateMultiLineConfidence(String combo, String keyword, String firstLine, String restLines) {
        double confidence = 0.6;

        if (firstLine.length() < 20 && restLines.length() < 30) confidence += 0.2;
        if (combo.contains("expérience professionnelle") ||
            combo.contains("work experience") ||
            combo.contains("professional experience")) confidence += 0.2;

        return Math.min(confidence, 1.0);
    }

    private double calculateBulletedConfidence(String content, String keyword) {
        double confidence = 0.6;
        if (content.equals(keyword.toLowerCase())) confidence += 0.3;
        if (content.length() < 25) confidence += 0.1;
        return Math.min(confidence, 1.0);
    }

    private double calculateIndentedConfidence(String content, String keyword) {
        double confidence = 0.6;
        if (content.equals(keyword.toLowerCase())) confidence += 0.3;
        if (content.length() < 25) confidence += 0.1;
        return Math.min(confidence, 1.0);
    }

    /**
     * Header candidate class for multi-stage detection
     */
    private static class HeaderCandidate {
        String text;
        int startLineIndex;
        int endLineIndex;
        HeaderType type;
        double confidence;
        String matchedKeyword;

        HeaderCandidate(String text, int startLine, int endLine, HeaderType type, double confidence, String keyword) {
            this.text = text;
            this.startLineIndex = startLine;
            this.endLineIndex = endLine;
            this.type = type;
            this.confidence = confidence;
            this.matchedKeyword = keyword;
        }
    }

    /**
     * Header type enumeration
     */
    private enum HeaderType {
        SINGLE_LINE,     // "EXPERIENCE"
        MULTI_LINE,      // "EXPÉRIENCE\nPROFESSIONNELLE"
        BULLETED,        // "• Work History"
        INDENTED,        // "    FORMATION"
        UNDERLINED       // "EXPERIENCE\n----------"
    }
}