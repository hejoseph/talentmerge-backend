package com.talentmerge.dto;

import com.talentmerge.validation.ValidPhoneNumber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateCreateRequestDTO {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;
    
    @ValidPhoneNumber(message = "Phone number format is invalid")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;
    
    @Size(max = 5000, message = "Skills description cannot exceed 5000 characters")
    private String skills;
    
    @Valid
    private List<WorkExperienceCreateDTO> workExperiences = new ArrayList<>();
    
    @Valid
    private List<EducationCreateDTO> educations = new ArrayList<>();
}