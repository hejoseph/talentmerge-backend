package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("ai")
public class AiParsingService  implements IParsingService{
    @Override
    public Candidate parseCandidateFromText(String text) {
        return null;
    }
}
