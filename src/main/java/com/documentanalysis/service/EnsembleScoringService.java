package com.documentanalysis.service;

import com.documentanalysis.model.AnalysisResult;
import com.documentanalysis.model.EnsembleScore;
import com.documentanalysis.model.ModuleScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnsembleScoringService {

    // Exact weights from prompt
    private static final double METADATA_WEIGHT = 0.20;
    private static final double FORENSICS_WEIGHT = 0.40;
    private static final double VISUAL_WEIGHT = 0.10;
    private static final double DEEPFAKE_WEIGHT = 0.30;

    public AnalysisResult calculateEnsembleScore(
            ModuleScore metadataScore,
            ModuleScore forensicsScore,
            ModuleScore visualScore,
            ModuleScore deepfakeScore) {
        
        AnalysisResult result = new AnalysisResult();
        
        // Set module scores
        AnalysisResult.ModuleScores moduleScores = new AnalysisResult.ModuleScores();
        moduleScores.setMetadata(metadataScore);
        moduleScores.setForensics(forensicsScore);
        moduleScores.setVisualAnomalies(visualScore);
        moduleScores.setDeepfakeDetection(deepfakeScore);
        result.setModuleScores(moduleScores);
        
        // Calculate weighted ensemble score
        double finalScore = (metadataScore.getScore() * METADATA_WEIGHT) +
                           (forensicsScore.getScore() * FORENSICS_WEIGHT) +
                           (visualScore.getScore() * VISUAL_WEIGHT) +
                           (deepfakeScore.getScore() * DEEPFAKE_WEIGHT);
        
        // Calculate average confidence
        double avgConfidence = (metadataScore.getConfidence() + 
                               forensicsScore.getConfidence() + 
                               visualScore.getConfidence() + 
                               deepfakeScore.getConfidence()) / 4.0;
        
        // Determine risk level
        String riskLevel;
        String interpretation;
        
        if (finalScore > 75) {
            riskLevel = "HIGH";
            interpretation = "Very likely manipulated - multiple strong indicators detected";
        } else if (finalScore >= 50) {
            riskLevel = "MEDIUM";
            interpretation = "Likely manipulated - moderate indicators detected";
        } else {
            riskLevel = "LOW";
            interpretation = "Unlikely manipulated - few indicators detected";
        }
        
        EnsembleScore ensembleScore = new EnsembleScore();
        ensembleScore.setFinalScore(finalScore);
        ensembleScore.setRiskLevel(riskLevel);
        ensembleScore.setConfidence(avgConfidence);
        ensembleScore.setInterpretation(interpretation);
        
        result.setEnsembleScore(ensembleScore);
        
        log.info("Ensemble score calculated: {}/100 ({}), Confidence: {}", 
                finalScore, riskLevel, avgConfidence);
        
        return result;
    }
}