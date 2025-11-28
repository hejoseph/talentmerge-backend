package com.talentmerge.service;

import java.io.IOException;
import java.nio.file.Path;

public interface ParsingService {
    String parse(Path filePath) throws IOException;
}