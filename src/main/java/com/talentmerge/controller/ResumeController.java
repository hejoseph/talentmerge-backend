package com.talentmerge.controller;

import com.talentmerge.service.FileStorageService;
import com.talentmerge.service.ParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final FileStorageService fileStorageService;
    private final ParsingService parsingService;

    @Autowired
    public ResumeController(FileStorageService fileStorageService, ParsingService parsingService) {
        this.fileStorageService = fileStorageService;
        this.parsingService = parsingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }
        try {
            String fileName = fileStorageService.storeFile(file);
            Path filePath = fileStorageService.getFile(fileName);
            String content = parsingService.parse(filePath);
            return ResponseEntity.ok(content);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Could not process the file: " + ex.getMessage());
        }
    }
}