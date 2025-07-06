package com.example.mapper.service;

import com.example.mapper.model.SourceReliability;
import com.example.mapper.repo.SourceReliabilityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SourceReliabilityService {
    private final SourceReliabilityRepository repo;

    public SourceReliabilityService(SourceReliabilityRepository repo) {
        this.repo = repo;
    }

    public SourceReliability getOrCreate(String source) {
        return repo.findById(source).orElseGet(() -> repo.save(new SourceReliability(source, 0.8)));
    }

    public void updateReliability(String source, boolean correct) {
        SourceReliability sr = getOrCreate(source);
        sr.setClaimCount(sr.getClaimCount() + 1);
        if (correct) sr.setCorrectCount(sr.getCorrectCount() + 1);
        // Simple reliability: correct/total
        sr.setReliability(sr.getClaimCount() == 0 ? 0.8 : ((double) sr.getCorrectCount()) / sr.getClaimCount());
        repo.save(sr);
    }

    public double getReliability(String source) {
        return getOrCreate(source).getReliability();
    }

    public List<SourceReliability> getAllReliability() {
        return repo.findAll();
    }
}
