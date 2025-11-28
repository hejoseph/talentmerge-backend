package com.talentmerge.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfBoxAndPoiParsingService implements ParsingService {

    @Override
    public String parse(Path filePath) throws IOException {
        String contentType = Files.probeContentType(filePath);

        if (contentType == null) {
            throw new IOException("Could not determine file type.");
        }

        switch (contentType) {
            case "application/pdf":
                return parsePdf(filePath);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return parseDocx(filePath);
            default:
                throw new IOException("Unsupported file type: " + contentType);
        }
    }

    private String parsePdf(Path filePath) throws IOException {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(Path filePath) throws IOException {
        try (InputStream stream = Files.newInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(stream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}