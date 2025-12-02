package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import java.io.InputStream;

public interface ParsingService {
    String parseResume(InputStream inputStream, String contentType);
    Candidate parseCandidateFromText(String text);
}