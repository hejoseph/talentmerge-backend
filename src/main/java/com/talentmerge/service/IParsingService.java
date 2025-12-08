package com.talentmerge.service;

import com.talentmerge.model.Candidate;

public interface IParsingService {
    Candidate parseCandidateFromText(String text);
}
