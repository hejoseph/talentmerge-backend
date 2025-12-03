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
        List<HeaderCandidate> validatedHeaders = validateHeaders(candidates, lines);

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
        List<HeaderCandidate> candidates = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String currentLine = lines[i].trim();
            if (currentLine.isEmpty()) continue;

            // Check single-line headers
            HeaderCandidate singleLine = checkSingleLineHeader(currentLine, i);
            if (singleLine != null) {
                candidates.add(singleLine);
            }

            // Check multi-line headers (look ahead 1-2 lines)
            HeaderCandidate multiLine = checkMultiLineHeader(lines, i);
            if (multiLine != null) {
                candidates.add(multiLine);
                // Skip the lines we've already processed in the multi-line header
                i = multiLine.endLineIndex;
            }

            // Check bulleted headers
            HeaderCandidate bulleted = checkBulletedHeader(currentLine, i);
            if (bulleted != null) {
                candidates.add(bulleted);
            }

            // Check indented headers
            HeaderCandidate indented = checkIndentedHeader(lines[i], i);
            if (indented != null) {
                candidates.add(indented);
            }
        }

        return candidates;
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
     * Stage 3: Validate headers (placeholder - will be enhanced in next step)
     */
    private List<HeaderCandidate> validateHeaders(List<HeaderCandidate> candidates, String[] lines) {
        // For now, just return all candidates above confidence threshold
        return candidates.stream()
                .filter(candidate -> candidate.confidence > 0.6)
                .sorted((a, b) -> Integer.compare(a.startLineIndex, b.startLineIndex))
                .toList();
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