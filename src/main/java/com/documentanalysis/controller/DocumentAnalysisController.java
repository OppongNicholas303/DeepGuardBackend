package com.documentanalysis.controller;

import com.documentanalysis.model.AnalysisResult;
import com.documentanalysis.service.DocumentAnalysisService;
import com.documentanalysis.util.FileValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/document-analysis")
public class DocumentAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisController.class);

    @Autowired
    private DocumentAnalysisService documentAnalysisService;
    
    @Autowired
    private FileValidationUtil fileValidationUtil;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyzeDocument(
            @RequestParam("file") MultipartFile file) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate file
            fileValidationUtil.validateFile(file);
            
            // Analyze (runs all 4 modules in parallel + Gemini)
            AnalysisResult result = documentAnalysisService.analyze(file);
            
            long processingTime = System.currentTimeMillis() - startTime;
            result.setProcessingTimeMs(processingTime);
            result.setStatus("COMPLETED");
            result.setTimestamp(java.time.Instant.now());
            
            log.info("Analysis completed in {}ms for file: {}", processingTime, file.getOriginalFilename());
            
            // Return immediately (no polling needed)
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Analysis failed for file: {}", file.getOriginalFilename(), e);
            
            AnalysisResult errorResult = new AnalysisResult();
            errorResult.setStatus("ERROR");
            errorResult.setTimestamp(java.time.Instant.now());
            errorResult.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
}