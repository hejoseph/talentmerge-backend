package com.talentmerge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String skills;
    private String originalFilePath;
}
