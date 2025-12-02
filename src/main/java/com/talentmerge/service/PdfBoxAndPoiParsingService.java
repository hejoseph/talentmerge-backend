package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfBoxAndPoiParsingService implements ParsingService {

    @Override
    public String parseResume(InputStream inputStream, String contentType) {
        try {
            if (contentType == null) {
                throw new IOException("Could not determine file type.");
            }

            switch (contentType) {
                case "application/pdf":
                    return parsePdf(inputStream);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    return parseDocx(inputStream);
                default:
                    // Optionally, handle unsupported file types or return a specific message
                    return "Unsupported file type: " + contentType;
            }
        } catch (IOException e) {
            // Log the exception and return an error message
            // For a real application, you'd use a logging framework
            e.printStackTrace();
            return "Error parsing resume: " + e.getMessage();
        }
    }

    private String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    @Override
    public Candidate parseCandidateFromText(String text) {
        Candidate candidate = new Candidate();
        candidate.setName(extractName(text));
        candidate.setEmail(extractEmail(text));
        candidate.setPhone(extractPhone(text));
        // Skills extraction can be implemented later
        candidate.setSkills("");
        return candidate;
    }

    private String extractName(String text) {
        // This is a simple heuristic and might need to be improved.
        // It assumes the name is one of the first lines of the resume.
        Pattern pattern = Pattern.compile("^([A-Z][a-z]+(?:\\s[A-Z][a-z]+)+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "N/A";
    }

    private String extractEmail(String text) {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "N/A";
    }

    private String extractPhone(String text) {
        Pattern pattern = Pattern.compile("(\\+?\\d{1,3}[-\\.\\s]?)?\\(?\\d{3}\\)?[-\\.\\s]?\\d{3}[-\\.\\s]?\\d{4}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return "N/A";
    }
}