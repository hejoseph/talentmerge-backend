package com.talentmerge.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ResumeParsingService {

    public String parseResume(Path filePath) throws IOException {
        try (InputStream stream = Files.newInputStream(filePath)) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            parser.parse(stream, handler, metadata);
            return handler.toString();
        } catch (Exception e) {
            throw new IOException("Failed to parse resume file: " + e.getMessage(), e);
        }
    }
}