package com.mambogo.gateway.sanitization;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of threat analysis containing threat score, detected threats, and metadata.
 * 
 * @author Prashant Sinha
 * @since SEC-11 Implementation
 */
public class ThreatAnalysisResult {
    
    private final int threatScore;
    private final List<ThreatDetail> threats;
    private final boolean threatDetected;
    private final long analysisTimeNanos;

    public ThreatAnalysisResult(int threatScore, List<ThreatDetail> threats, boolean threatDetected) {
        this(threatScore, threats, threatDetected, 0);
    }
    
    public ThreatAnalysisResult(int threatScore, List<ThreatDetail> threats, boolean threatDetected, long analysisTimeNanos) {
        this.threatScore = threatScore;
        this.threats = List.copyOf(threats);
        this.threatDetected = threatDetected;
        this.analysisTimeNanos = analysisTimeNanos;
    }

    public int getThreatScore() {
        return threatScore;
    }

    public List<ThreatDetail> getThreats() {
        return threats;
    }

    public boolean isThreatDetected() {
        return threatDetected;
    }

    public long getAnalysisTimeNanos() {
        return analysisTimeNanos;
    }

    /**
     * Get a summary of detected threats as a comma-separated string
     */
    public String getThreatSummary() {
        if (threats.isEmpty()) {
            return "No threats detected";
        }
        
        StringBuilder summary = new StringBuilder();
        for (ThreatDetail threat : threats) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(threat.getType()).append("(").append(threat.getScore()).append(")");
        }
        
        return summary.toString();
    }

    /**
     * Builder class for constructing ThreatAnalysisResult
     */
    public static class Builder {
        private int totalScore = 0;
        private final List<ThreatDetail> threats = new ArrayList<>();
        private long startTime = System.nanoTime();

        public Builder addThreat(String type, int score, String description) {
            threats.add(new ThreatDetail(type, score, description));
            totalScore += score;
            return this;
        }

        public ThreatAnalysisResult build() {
            long analysisTime = System.nanoTime() - startTime;
            boolean threatDetected = !threats.isEmpty();
            return new ThreatAnalysisResult(totalScore, threats, threatDetected, analysisTime);
        }
    }

    /**
     * Details of a specific threat detected in the input
     */
    public static class ThreatDetail {
        private final String type;
        private final int score;
        private final String description;

        public ThreatDetail(String type, int score, String description) {
            this.type = type;
            this.score = score;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public int getScore() {
            return score;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return String.format("%s(%d): %s", type, score, description);
        }
    }
}
