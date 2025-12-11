package com.talentmerge.dto;

import lombok.Data;

@Data
public class ResumeParseRequest {
    private String rawText;
    private String originalFilePath; // optional
}
