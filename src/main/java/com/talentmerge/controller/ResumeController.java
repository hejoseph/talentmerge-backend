package com.talentmerge.controller;

import com.talentmerge.service.FileStorageService;
import com.talentmerge.service.ParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

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
    public ResponseEntity<Map<String, String>> uploadResume(@RequestParam("file") MultipartFile file) {
        System.out.println("UPLOAD CONTROLLER REACHED, FILE = " + file.getOriginalFilename());
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Please select a file to upload."));
        }
        try {
            String fileName = fileStorageService.storeFile(file);
            Path filePath = fileStorageService.getFile(fileName);
            String content = parsingService.parse(filePath);
            return ResponseEntity.ok(Collections.singletonMap("text", content));
        } catch (IOException ex) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Could not process the file: " + ex.getMessage()));
        }
    }
}