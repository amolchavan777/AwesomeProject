package com.example.mapper.service.metrics;

import com.example.mapper.model.DependencyClaim;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Service to compute evaluation metrics (precision, recall, F1) for dependency claims.
 * Assumes a set of ground truth claims and a set of predicted/resolved claims.
 */
@Service
public class EvaluationMetricsService {
    public static class Metrics {
        public final double precision;
        public final double recall;
        public final double f1;
        public final int truePositives;
        public final int falsePositives;
        public final int falseNegatives;
        public Metrics(double precision, double recall, double f1, int tp, int fp, int fn) {
            this.precision = precision;
            this.recall = recall;
            this.f1 = f1;
            this.truePositives = tp;
            this.falsePositives = fp;
            this.falseNegatives = fn;
        }
    }

    /**
     * Compute metrics given ground truth and predicted claims.
     */
    public Metrics compute(List<DependencyClaim> groundTruth, List<DependencyClaim> predicted) {
        Set<String> gt = new HashSet<>();
        for (DependencyClaim c : groundTruth) {
            gt.add(c.getFromService().getName() + "->" + c.getToService().getName());
        }
        Set<String> pred = new HashSet<>();
        for (DependencyClaim c : predicted) {
            pred.add(c.getFromService().getName() + "->" + c.getToService().getName());
        }
        int tp = 0, fp = 0, fn = 0;
        for (String p : pred) {
            if (gt.contains(p)) tp++; else fp++;
        }
        for (String g : gt) {
            if (!pred.contains(g)) fn++;
        }
        double precision = tp + fp == 0 ? 0 : (double)tp / (tp + fp);
        double recall = tp + fn == 0 ? 0 : (double)tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        return new Metrics(precision, recall, f1, tp, fp, fn);
    }
}
