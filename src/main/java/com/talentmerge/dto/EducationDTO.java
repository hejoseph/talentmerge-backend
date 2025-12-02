package com.talentmerge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EducationDTO {
    private Long id;
    private String institution;
    private String degree;
    private LocalDate graduationDate;
}
