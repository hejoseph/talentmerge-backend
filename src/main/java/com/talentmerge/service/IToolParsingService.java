package com.talentmerge.service;

import java.io.InputStream;

public interface IToolParsingService {
    String parseResume(InputStream inputStream, String contentType);
}