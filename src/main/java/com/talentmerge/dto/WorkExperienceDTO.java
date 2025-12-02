package com.talentmerge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkExperienceDTO {
    private Long id;
    private String jobTitle;
    private String company;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
