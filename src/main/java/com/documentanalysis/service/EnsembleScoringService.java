package com.documentanalysis.service;

import com.documentanalysis.model.AnalysisResult;
import com.documentanalysis.model.EnsembleScore;
import com.documentanalysis.model.ModuleScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EnsembleScoringService {

    private static final Logger log = LoggerFactory.getLogger(EnsembleScoringService.class);

    // Image analysis weights (original)
    private static final double IMAGE_METADATA_WEIGHT = 0.20;
    private static final double IMAGE_FORENSICS_WEIGHT = 0.40;
    private static final double IMAGE_VISUAL_WEIGHT = 0.10;
    private static final double IMAGE_DEEPFAKE_WEIGHT = 0.30;
    
    // Reduced metadata weight for screenshots/exports
    private static final double SCREENSHOT_METADATA_WEIGHT = 0.05;
    private static final double SCREENSHOT_FORENSICS_WEIGHT = 0.45;
    private static final double SCREENSHOT_VISUAL_WEIGHT = 0.15;
    private static final double SCREENSHOT_DEEPFAKE_WEIGHT = 0.35;

    // PDF analysis weights (new)
    private static final double PDF_METADATA_WEIGHT = 0.25;
    private static final double PDF_FORENSICS_WEIGHT = 0.30;
    private static final double PDF_VISUAL_WEIGHT = 0.15;
    private static final double PDF_TEXT_WEIGHT = 0.30;

    public AnalysisResult calculateEnsembleScore(
            ModuleScore metadataScore,
            ModuleScore forensicsScore,
            ModuleScore visualScore,
            ModuleScore deepfakeScore) {
        
        AnalysisResult result = new AnalysisResult();
        
        // Set module scores for images
        AnalysisResult.ModuleScores moduleScores = new AnalysisResult.ModuleScores();
        moduleScores.setMetadata(metadataScore);
        moduleScores.setForensics(forensicsScore);
        moduleScores.setVisualAnomalies(visualScore);
        moduleScores.setDeepfakeDetection(deepfakeScore);
        result.setModuleScores(moduleScores);
        
        // Determine if document is screenshot/export (missing camera metadata)
        boolean isScreenshotOrExport = isScreenshotOrExport(metadataScore);
        
        // Calculate weighted ensemble score for images
        double finalScore;
        if (isScreenshotOrExport) {
            finalScore = (metadataScore.getScore() * SCREENSHOT_METADATA_WEIGHT) +
                        (forensicsScore.getScore() * SCREENSHOT_FORENSICS_WEIGHT) +
                        (visualScore.getScore() * SCREENSHOT_VISUAL_WEIGHT) +
                        (deepfakeScore.getScore() * SCREENSHOT_DEEPFAKE_WEIGHT);
        } else {
            finalScore = (metadataScore.getScore() * IMAGE_METADATA_WEIGHT) +
                        (forensicsScore.getScore() * IMAGE_FORENSICS_WEIGHT) +
                        (visualScore.getScore() * IMAGE_VISUAL_WEIGHT) +
                        (deepfakeScore.getScore() * IMAGE_DEEPFAKE_WEIGHT);
        }
        
        // Calculate average confidence
        double avgConfidence = (metadataScore.getConfidence() + 
                               forensicsScore.getConfidence() + 
                               visualScore.getConfidence() + 
                               deepfakeScore.getConfidence()) / 4.0;
        
        EnsembleScore ensembleScore = createEnsembleScore(finalScore, avgConfidence);
        result.setEnsembleScore(ensembleScore);
        
        log.info("Image ensemble score calculated: {}/100 ({}), Confidence: {}", 
                finalScore, ensembleScore.getRiskLevel(), avgConfidence);
        
        return result;
    }

    public AnalysisResult calculatePdfEnsembleScore(
            ModuleScore metadataScore,
            ModuleScore forensicsScore,
            ModuleScore visualScore,
            ModuleScore textScore) {
        
        AnalysisResult result = new AnalysisResult();
        
        // Set module scores for PDFs (including text manipulation)
        AnalysisResult.ModuleScores moduleScores = new AnalysisResult.ModuleScores();
        moduleScores.setMetadata(metadataScore);
        moduleScores.setForensics(forensicsScore);
        moduleScores.setVisualAnomalies(visualScore);
        moduleScores.setTextManipulation(textScore); // PDF-specific module
        result.setModuleScores(moduleScores);
        
        // Calculate weighted ensemble score for PDFs
        double finalScore = (metadataScore.getScore() * PDF_METADATA_WEIGHT) +
                           (forensicsScore.getScore() * PDF_FORENSICS_WEIGHT) +
                           (visualScore.getScore() * PDF_VISUAL_WEIGHT) +
                           (textScore.getScore() * PDF_TEXT_WEIGHT);
        
        // Calculate average confidence (4 modules for PDF)
        double avgConfidence = (metadataScore.getConfidence() + 
                               forensicsScore.getConfidence() + 
                               visualScore.getConfidence() + 
                               textScore.getConfidence()) / 4.0;
        
        EnsembleScore ensembleScore = createEnsembleScore(finalScore, avgConfidence);
        result.setEnsembleScore(ensembleScore);
        
        log.info("PDF ensemble score calculated: {}/100 ({}), Confidence: {}", 
                finalScore, ensembleScore.getRiskLevel(), avgConfidence);
        
        return result;
    }

    private EnsembleScore createEnsembleScore(double finalScore, double avgConfidence) {
        // Determine risk level and interpretation
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
        
        return ensembleScore;
    }
    
    private boolean isScreenshotOrExport(ModuleScore metadataScore) {
        // Check findings for indicators of screenshots or exported documents
        return metadataScore.getFindings().stream().anyMatch(finding -> 
            finding.toLowerCase().contains("missing exif") ||
            finding.toLowerCase().contains("missing critical metadata") ||
            finding.toLowerCase().contains("screenshot") ||
            finding.toLowerCase().contains("export") ||
            finding.toLowerCase().contains("converted")
        );
    }
}