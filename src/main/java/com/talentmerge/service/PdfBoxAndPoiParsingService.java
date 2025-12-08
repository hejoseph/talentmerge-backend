package com.talentmerge.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfBoxAndPoiParsingService implements IToolParsingService {

    public PdfBoxAndPoiParsingService() {
    }



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
                    return "Unsupported file type: " + contentType;
            }
        } catch (IOException e) {
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



    


}