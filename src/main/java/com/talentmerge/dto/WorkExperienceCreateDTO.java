package com.talentmerge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkExperienceCreateDTO {
    
    @NotBlank(message = "Job title is required")
    @Size(max = 100, message = "Job title cannot exceed 100 characters")
    private String jobTitle;
    
    @NotBlank(message = "Company name is required")
    @Size(max = 100, message = "Company name cannot exceed 100 characters")
    private String company;
    
    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;
    
    @PastOrPresent(message = "End date cannot be in the future")
    private LocalDate endDate; // Can be null for current positions
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    // Custom validation method to ensure end date is after start date
    public boolean isEndDateValid() {
        if (endDate == null) {
            return true; // Current position
        }
        return !endDate.isBefore(startDate);
    }
}