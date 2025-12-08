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
public class EducationCreateDTO {
    
    @NotBlank(message = "Institution name is required")
    @Size(max = 100, message = "Institution name cannot exceed 100 characters")
    private String institution;
    
    @NotBlank(message = "Degree is required")
    @Size(max = 100, message = "Degree cannot exceed 100 characters")
    private String degree;
    
    @NotNull(message = "Graduation date is required")
    @PastOrPresent(message = "Graduation date cannot be in the future")
    private LocalDate graduationDate;
}