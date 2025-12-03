package com.talentmerge.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced date parsing service with comprehensive validation and analysis
 * Handles multiple date formats, chronological validation, and gap detection
 */
@Service
public class DateParsingService {

    // Enhanced date patterns with better capture groups
    private static final List<DatePattern> DATE_PATTERNS = Arrays.asList(
        // English patterns
        new DatePattern(
            Pattern.compile("(\\w{3,9})\\s+(\\d{4})\\s*[-–—]\\s*(\\w{3,9})\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
            "MONTH_YEAR_TO_MONTH_YEAR", // "January 2020 - December 2022"
            Arrays.asList(1, 2, 3, 4)
        ),
        new DatePattern(
            Pattern.compile("(\\d{1,2})/(\\d{4})\\s*[-–—]\\s*(\\d{1,2})/(\\d{4})", Pattern.CASE_INSENSITIVE),
            "MM_YYYY_TO_MM_YYYY", // "01/2020 - 12/2022"
            Arrays.asList(1, 2, 3, 4)
        ),
        new DatePattern(
            Pattern.compile("(\\d{4})[-.]?(\\d{2})\\s*[-–—]\\s*(\\d{4})[-.]?(\\d{2})", Pattern.CASE_INSENSITIVE),
            "YYYY_MM_TO_YYYY_MM", // "2020-01 - 2022-12"
            Arrays.asList(1, 2, 3, 4) // Fixed: correct order for YYYY-MM format
        ),
        new DatePattern(
            Pattern.compile("(\\w{3,9})\\s+(\\d{4})\\s*[-–—]\\s*(present|current|now)", Pattern.CASE_INSENSITIVE),
            "MONTH_YEAR_TO_PRESENT", // "January 2020 - Present"
            Arrays.asList(1, 2, -1, -1)
        ),
        
        // French patterns
        new DatePattern(
            Pattern.compile("(janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre|janv|févr|avr|juil|sept|oct|nov|déc)\\s+(\\d{4})\\s*[-–—]\\s*(janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre|janv|févr|avr|juil|sept|oct|nov|déc)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
            "FRENCH_MONTH_YEAR_TO_MONTH_YEAR", // "janvier 2020 - décembre 2022"
            Arrays.asList(1, 2, 3, 4)
        ),
        new DatePattern(
            Pattern.compile("(\\w{3,9})\\s+(\\d{4})\\s*[-–—]\\s*(aujourd'hui|actuel|maintenant)", Pattern.CASE_INSENSITIVE),
            "FRENCH_MONTH_YEAR_TO_PRESENT", // "janvier 2020 - Aujourd'hui"
            Arrays.asList(1, 2, -1, -1)
        ),
        new DatePattern(
            Pattern.compile("du\\s+(\\w{3,9})\\s+(\\d{4})\\s+au\\s+(\\w{3,9})\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
            "FRENCH_DU_AU", // "du janvier 2020 au décembre 2022"
            Arrays.asList(1, 2, 3, 4)
        ),
        new DatePattern(
            Pattern.compile("de\\s+(\\d{1,2})/(\\d{4})\\s+[àa]\\s+(\\d{1,2})/(\\d{4})", Pattern.CASE_INSENSITIVE),
            "FRENCH_DE_A", // "de 01/2020 à 12/2022"
            Arrays.asList(1, 2, 3, 4)
        ),
        
        // Additional flexible patterns
        new DatePattern(
            Pattern.compile("(\\d{4})\\s*[-–—]\\s*(\\d{4})", Pattern.CASE_INSENSITIVE),
            "YEAR_TO_YEAR", // "2020 - 2022"
            Arrays.asList(-1, 1, -1, 2)
        ),
        new DatePattern(
            Pattern.compile("(\\d{4})\\s*[-–—]\\s*(present|current|aujourd'hui|actuel)", Pattern.CASE_INSENSITIVE),
            "YEAR_TO_PRESENT", // "2020 - Present"
            Arrays.asList(-1, 1, -1, -1)
        ),
        
        // Quarter patterns
        new DatePattern(
            Pattern.compile("Q(\\d)\\s+(\\d{4})\\s*[-–—]\\s*Q(\\d)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
            "QUARTER_TO_QUARTER", // "Q1 2020 - Q4 2022"  
            Arrays.asList(1, 2, 3, 4)
        )
    );

    // Month name mappings for English and French
    private static final Map<String, Integer> MONTH_MAPPINGS = new HashMap<>();
    
    static {
        // English months
        MONTH_MAPPINGS.put("jan", 1); MONTH_MAPPINGS.put("january", 1);
        MONTH_MAPPINGS.put("feb", 2); MONTH_MAPPINGS.put("february", 2);
        MONTH_MAPPINGS.put("mar", 3); MONTH_MAPPINGS.put("march", 3);
        MONTH_MAPPINGS.put("apr", 4); MONTH_MAPPINGS.put("april", 4);
        MONTH_MAPPINGS.put("may", 5);
        MONTH_MAPPINGS.put("jun", 6); MONTH_MAPPINGS.put("june", 6);
        MONTH_MAPPINGS.put("jul", 7); MONTH_MAPPINGS.put("july", 7);
        MONTH_MAPPINGS.put("aug", 8); MONTH_MAPPINGS.put("august", 8);
        MONTH_MAPPINGS.put("sep", 9); MONTH_MAPPINGS.put("september", 9); MONTH_MAPPINGS.put("sept", 9);
        MONTH_MAPPINGS.put("oct", 10); MONTH_MAPPINGS.put("october", 10);
        MONTH_MAPPINGS.put("nov", 11); MONTH_MAPPINGS.put("november", 11);
        MONTH_MAPPINGS.put("dec", 12); MONTH_MAPPINGS.put("december", 12);
        
        // French months
        MONTH_MAPPINGS.put("janv", 1); MONTH_MAPPINGS.put("janvier", 1);
        MONTH_MAPPINGS.put("févr", 2); MONTH_MAPPINGS.put("février", 2); MONTH_MAPPINGS.put("fev", 2); MONTH_MAPPINGS.put("fevrier", 2);
        MONTH_MAPPINGS.put("mars", 3);
        MONTH_MAPPINGS.put("avr", 4); MONTH_MAPPINGS.put("avril", 4);
        MONTH_MAPPINGS.put("mai", 5);
        MONTH_MAPPINGS.put("juin", 6);
        MONTH_MAPPINGS.put("juil", 7); MONTH_MAPPINGS.put("juillet", 7);
        MONTH_MAPPINGS.put("août", 8); MONTH_MAPPINGS.put("aout", 8);
        MONTH_MAPPINGS.put("sept", 9); MONTH_MAPPINGS.put("septembre", 9);
        MONTH_MAPPINGS.put("oct", 10); MONTH_MAPPINGS.put("octobre", 10);
        MONTH_MAPPINGS.put("nov", 11); MONTH_MAPPINGS.put("novembre", 11);
        MONTH_MAPPINGS.put("déc", 12); MONTH_MAPPINGS.put("décembre", 12); MONTH_MAPPINGS.put("dec", 12); MONTH_MAPPINGS.put("decembre", 12);
    }

    /**
     * Parse date range with comprehensive validation
     */
    public DateRangeResult parseDateRange(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return new DateRangeResult(null, null, false, "Empty date text");
        }

        String cleanedText = preprocessDateText(dateText);
        
        for (DatePattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.pattern.matcher(cleanedText);
            if (matcher.find()) {
                
                try {
                    DateRangeResult result = parseWithPattern(matcher, pattern, cleanedText);
                    if (result != null && result.isValid) {
                        return validateDateRange(result);
                    }
                } catch (Exception e) {
                    // Continue to next pattern
                    continue;
                }
            }
        }
        
        return new DateRangeResult(null, null, false, "No matching date pattern found");
    }

    /**
     * Preprocess date text to handle common variations
     */
    private String preprocessDateText(String dateText) {
        String cleaned = dateText.toLowerCase()
                      .replaceAll("[,.]", "") // Remove commas and periods
                      .replaceAll("\\s+", " ") // Normalize whitespace
                      .replaceAll("–", "-") // Normalize dashes  
                      .replaceAll("—", "-")
                      .replaceAll("\\bto\\b", " - ") // Convert "to" to dash with spaces
                      .replaceAll("\\btill?\\b", " - ") // Convert "till"/"til" to dash
                      .trim();
        
        
        return cleaned;
    }

    /**
     * Parse date range using specific pattern
     */
    private DateRangeResult parseWithPattern(Matcher matcher, DatePattern pattern, String originalText) {
        List<Integer> groups = pattern.groupOrder;
        
        // Extract components based on pattern
        String startMonth = extractGroup(matcher, groups.get(0));
        String startYear = extractGroup(matcher, groups.get(1));
        String endMonth = extractGroup(matcher, groups.get(2));
        String endYear = extractGroup(matcher, groups.get(3));
        
        // Handle special cases for YYYY-MM format
        if (pattern.type.equals("YYYY_MM_TO_YYYY_MM")) {
            // For YYYY-MM format, we need to swap positions
            String tempStartYear = startMonth;  // Group 1 is year
            String tempStartMonth = startYear;  // Group 2 is month
            String tempEndYear = endMonth;      // Group 3 is year  
            String tempEndMonth = endYear;      // Group 4 is month
            
            startMonth = tempStartMonth;
            startYear = tempStartYear;
            endMonth = tempEndMonth;
            endYear = tempEndYear;
        }
        
        // Handle special cases
        boolean isOngoing = isOngoingPosition(originalText);
        
        try {
            LocalDate startDate = parseDate(startMonth, startYear, pattern.type);
            LocalDate endDate = isOngoing ? null : parseDate(endMonth, endYear, pattern.type);
            
            return new DateRangeResult(startDate, endDate, true, "Parsed with pattern: " + pattern.type);
            
        } catch (Exception e) {
            return new DateRangeResult(null, null, false, "Failed to parse dates: " + e.getMessage());
        }
    }

    /**
     * Extract group from matcher, handling special values
     */
    private String extractGroup(Matcher matcher, int groupIndex) {
        if (groupIndex == -1) return null; // Indicates no group for this component
        try {
            return matcher.group(groupIndex);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if position is ongoing
     */
    private boolean isOngoingPosition(String text) {
        String lower = text.toLowerCase();
        return lower.contains("present") || lower.contains("current") || lower.contains("now") ||
               lower.contains("aujourd'hui") || lower.contains("actuel") || lower.contains("maintenant");
    }

    /**
     * Parse individual date with context from pattern type
     */
    private LocalDate parseDate(String monthStr, String yearStr, String patternType) throws DateTimeParseException {
        if (yearStr == null) {
            throw new DateTimeParseException("Year is required", "", 0);
        }
        
        int year;
        try {
            year = Integer.parseInt(yearStr.trim());
            // Handle 2-digit years
            if (year < 100) {
                year += (year < 50) ? 2000 : 1900;
            }
        } catch (NumberFormatException e) {
            throw new DateTimeParseException("Invalid year format", yearStr, 0);
        }
        
        int month = 1; // Default to January
        
        if (monthStr != null && !monthStr.trim().isEmpty()) {
            if (monthStr.matches("\\d+")) {
                // Numeric month
                month = Integer.parseInt(monthStr.trim());
                if (month < 1 || month > 12) {
                    throw new DateTimeParseException("Invalid month number", monthStr, 0);
                }
            } else if (patternType.contains("QUARTER")) {
                // Quarter to month conversion
                int quarter = Integer.parseInt(monthStr.trim());
                // Q1->Jan(1), Q2->Apr(4), Q3->Jul(7), Q4->Oct(10)
                month = (quarter - 1) * 3 + 1;
            } else {
                // Month name
                month = parseMonthName(monthStr);
            }
        } else if (patternType.contains("YYYY_MM")) {
            // For YYYY-MM format, monthStr is actually the month part
            // This should not happen with current patterns, but good to handle
            month = 1;
        }
        
        // Validate date
        if (year < 1950 || year > LocalDate.now().getYear() + 1) {
            throw new DateTimeParseException("Year out of reasonable range", yearStr, 0);
        }
        
        return YearMonth.of(year, month).atDay(1);
    }

    /**
     * Parse month name with better error handling
     */
    private int parseMonthName(String monthName) throws DateTimeParseException {
        if (monthName == null || monthName.trim().isEmpty()) {
            return 1; // Default to January
        }
        
        String cleaned = monthName.toLowerCase()
                                 .trim()
                                 .replaceAll("[.,]", ""); // Remove punctuation
        
        Integer month = MONTH_MAPPINGS.get(cleaned);
        if (month != null) {
            return month;
        }
        
        // Try partial matching for abbreviated forms
        for (Map.Entry<String, Integer> entry : MONTH_MAPPINGS.entrySet()) {
            if (entry.getKey().startsWith(cleaned) || cleaned.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        throw new DateTimeParseException("Unknown month name", monthName, 0);
    }

    /**
     * Validate date range with chronological and logical checks
     */
    private DateRangeResult validateDateRange(DateRangeResult result) {
        if (result.startDate == null) {
            return new DateRangeResult(null, null, false, "Start date is required");
        }
        
        List<String> validationMessages = new ArrayList<>();
        boolean isValid = true;
        
        // Check if start date is in the future
        if (result.startDate.isAfter(LocalDate.now())) {
            validationMessages.add("Start date is in the future");
            isValid = false;
        }
        
        // Check chronological order
        if (result.endDate != null) {
            if (result.endDate.isBefore(result.startDate)) {
                validationMessages.add("End date is before start date");
                isValid = false;
            }
            
            if (result.endDate.isAfter(LocalDate.now())) {
                validationMessages.add("End date is in the future");
                isValid = false;
            }
            
            // Check for unreasonably long positions
            long monthsBetween = ChronoUnit.MONTHS.between(result.startDate, result.endDate);
            if (monthsBetween > 600) { // 50 years
                validationMessages.add("Position duration seems unreasonably long (" + monthsBetween + " months)");
                // Don't mark as invalid, just warn
            }
            
            // Check for very short positions (less than a week - likely parsing error)
            long daysBetween = ChronoUnit.DAYS.between(result.startDate, result.endDate);
            if (daysBetween < 7) {
                validationMessages.add("Position duration seems very short (" + daysBetween + " days)");
                // Don't mark as invalid, short positions can be legitimate
            }
        }
        
        // Update validation status and messages
        String message = result.message;
        if (!validationMessages.isEmpty()) {
            message += "; Validation warnings: " + String.join(", ", validationMessages);
        }
        
        return new DateRangeResult(result.startDate, result.endDate, isValid, message);
    }

    /**
     * Analyze career timeline for gaps and overlaps
     */
    public CareerAnalysis analyzeCareerTimeline(List<DateRangeResult> dateRanges) {
        List<DateRangeResult> validRanges = dateRanges.stream()
            .filter(range -> range.isValid && range.startDate != null)
            .sorted((a, b) -> a.startDate.compareTo(b.startDate))
            .toList();
            
        CareerAnalysis analysis = new CareerAnalysis();
        
        if (validRanges.isEmpty()) {
            analysis.totalExperienceMonths = 0;
            analysis.hasGaps = false;
            analysis.hasOverlaps = false;
            return analysis;
        }
        
        // Calculate total experience and identify gaps/overlaps
        int totalMonths = 0;
        LocalDate previousEnd = null;
        
        for (int i = 0; i < validRanges.size(); i++) {
            DateRangeResult current = validRanges.get(i);
            LocalDate currentStart = current.startDate;
            LocalDate currentEnd = current.endDate != null ? current.endDate : LocalDate.now();
            
            // Add to total experience
            totalMonths += ChronoUnit.MONTHS.between(currentStart, currentEnd);
            
            // Check for gaps
            if (previousEnd != null) {
                long gapMonths = ChronoUnit.MONTHS.between(previousEnd, currentStart);
                if (gapMonths > 1) { // Gap longer than 1 month
                    analysis.hasGaps = true;
                    analysis.gaps.add(new CareerGap(previousEnd, currentStart, gapMonths));
                } else if (gapMonths < 0) { // Overlap
                    analysis.hasOverlaps = true;
                    analysis.overlaps.add(new CareerOverlap(currentStart, previousEnd, Math.abs(gapMonths)));
                }
            }
            
            previousEnd = currentEnd;
        }
        
        analysis.totalExperienceMonths = totalMonths;
        analysis.careerStartDate = validRanges.get(0).startDate;
        analysis.careerEndDate = validRanges.get(validRanges.size() - 1).endDate; // null if current
        
        return analysis;
    }

    /**
     * Supporting classes
     */
    public static class DateRangeResult {
        public final LocalDate startDate;
        public final LocalDate endDate;
        public final boolean isValid;
        public final String message;
        
        public DateRangeResult(LocalDate startDate, LocalDate endDate, boolean isValid, String message) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.isValid = isValid;
            this.message = message;
        }
    }
    
    private static class DatePattern {
        public final Pattern pattern;
        public final String type;
        public final List<Integer> groupOrder; // Order of groups: [startMonth, startYear, endMonth, endYear]
        
        public DatePattern(Pattern pattern, String type, List<Integer> groupOrder) {
            this.pattern = pattern;
            this.type = type;
            this.groupOrder = groupOrder;
        }
    }
    
    public static class CareerAnalysis {
        public int totalExperienceMonths = 0;
        public LocalDate careerStartDate;
        public LocalDate careerEndDate;
        public boolean hasGaps = false;
        public boolean hasOverlaps = false;
        public List<CareerGap> gaps = new ArrayList<>();
        public List<CareerOverlap> overlaps = new ArrayList<>();
    }
    
    public static class CareerGap {
        public final LocalDate gapStart;
        public final LocalDate gapEnd;
        public final long gapMonths;
        
        public CareerGap(LocalDate start, LocalDate end, long months) {
            this.gapStart = start;
            this.gapEnd = end;
            this.gapMonths = months;
        }
    }
    
    public static class CareerOverlap {
        public final LocalDate overlapStart;
        public final LocalDate overlapEnd;
        public final long overlapMonths;
        
        public CareerOverlap(LocalDate start, LocalDate end, long months) {
            this.overlapStart = start;
            this.overlapEnd = end;
            this.overlapMonths = months;
        }
    }
}