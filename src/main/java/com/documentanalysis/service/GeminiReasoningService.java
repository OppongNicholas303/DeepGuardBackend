package com.documentanalysis.service;

import com.documentanalysis.model.AnalysisResult;
import com.documentanalysis.model.GeminiAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import com.documentanalysis.model.GeminiAnalysis;

@Service
public class GeminiReasoningService {

    private static final Logger log = LoggerFactory.getLogger(GeminiReasoningService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    public GeminiAnalysis analyzeFindings(AnalysisResult analysisResult) {
        try {
            log.info("Starting Gemini API call with model: {}", geminiModel);
            
            String prompt = buildForensicPrompt(analysisResult);
            
            Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                    "temperature", 0.5,
                    "maxOutputTokens", 2000
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            String url = String.format(GEMINI_API_URL, geminiModel) + "?key=" + geminiApiKey;
            log.info("Calling Gemini API at: {}", url.replace(geminiApiKey, "***"));
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            log.info("Gemini API response status: {}", response.getStatusCode());
            
            return parseGeminiResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            log.error("Full error details:", e);
            return createFallbackAnalysis(analysisResult);
        }
    }

    private String buildForensicPrompt(AnalysisResult result) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a document forensics expert. Analyze these findings and provide a professional assessment:\n\n");
        
        // Add module scores and findings
        prompt.append(String.format("METADATA ANALYSIS (Score: %.0f/100):\n", result.getModuleScores().getMetadata().getScore()));
        result.getModuleScores().getMetadata().getFindings().forEach(finding -> 
            prompt.append("- ").append(finding).append("\n"));
        
        prompt.append(String.format("\nFORENSICS ANALYSIS (Score: %.0f/100):\n", result.getModuleScores().getForensics().getScore()));
        result.getModuleScores().getForensics().getFindings().forEach(finding -> 
            prompt.append("- ").append(finding).append("\n"));
        
        prompt.append(String.format("\nVISUAL ANOMALIES (Score: %.0f/100):\n", result.getModuleScores().getVisualAnomalies().getScore()));
        result.getModuleScores().getVisualAnomalies().getFindings().forEach(finding -> 
            prompt.append("- ").append(finding).append("\n"));
        
        // Add deepfake or text manipulation based on document type
        if (result.getModuleScores().getDeepfakeDetection() != null) {
            prompt.append(String.format("\nDEEPFAKE DETECTION (Score: %.0f/100):\n", result.getModuleScores().getDeepfakeDetection().getScore()));
            result.getModuleScores().getDeepfakeDetection().getFindings().forEach(finding -> 
                prompt.append("- ").append(finding).append("\n"));
        }
        
        if (result.getModuleScores().getTextManipulation() != null) {
            prompt.append(String.format("\nTEXT MANIPULATION (Score: %.0f/100):\n", result.getModuleScores().getTextManipulation().getScore()));
            result.getModuleScores().getTextManipulation().getFindings().forEach(finding -> 
                prompt.append("- ").append(finding).append("\n"));
        }
        
        prompt.append(String.format("\nENSEMBLE SCORE: %.1f/100 (%s Risk)\n\n", 
            result.getEnsembleScore().getFinalScore(), 
            result.getEnsembleScore().getRiskLevel()));
        
        prompt.append("Provide a JSON response with:\n");
        prompt.append("1. executiveSummary (2-3 sentences)\n");
        prompt.append("2. keyFindings (3-4 bullet points)\n");
        prompt.append("3. riskAssessment (detailed reasoning)\n");
        prompt.append("4. areasOfConcern (specific regions with coordinates if available)\n");
        prompt.append("5. geminiConfidence (0.0-1.0)\n");
        prompt.append("6. recommendations (actionable steps)\n\n");
        prompt.append("Be concise, professional, and technical. Focus on the strongest evidence.");
        
        return prompt.toString();
    }

    private GeminiAnalysis parseGeminiResponse(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            
            if (candidates != null && !candidates.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                
                if (parts != null && !parts.isEmpty()) {
                    String text = (String) parts.get(0).get("text");
                    
                    // Try to extract JSON from response
                    return parseJsonFromText(text);
                }
            }
            
            throw new RuntimeException("Invalid Gemini response format");
            
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }

    private GeminiAnalysis parseJsonFromText(String text) {
        try {
            // Find JSON in the text (between { and })
            int startIndex = text.indexOf('{');
            int endIndex = text.lastIndexOf('}');
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                String jsonText = text.substring(startIndex, endIndex + 1);
                JsonNode jsonNode = objectMapper.readTree(jsonText);
                
                GeminiAnalysis analysis = new GeminiAnalysis();
                analysis.setExecutiveSummary(jsonNode.path("executiveSummary").asText());
                analysis.setRiskAssessment(jsonNode.path("riskAssessment").asText());
                analysis.setGeminiConfidence(jsonNode.path("geminiConfidence").asDouble(0.8));
                
                // Parse key findings
                List<String> keyFindings = new ArrayList<>();
                JsonNode findingsNode = jsonNode.path("keyFindings");
                if (findingsNode.isArray()) {
                    findingsNode.forEach(node -> keyFindings.add(node.asText()));
                }
                analysis.setKeyFindings(keyFindings);
                
                // Parse recommendations
                List<String> recommendations = new ArrayList<>();
                JsonNode recNode = jsonNode.path("recommendations");
                if (recNode.isArray()) {
                    recNode.forEach(node -> recommendations.add(node.asText()));
                }
                analysis.setRecommendations(recommendations);
                
                // areasOfConcern will be populated from flaggedRegions in DocumentAnalysisService
                analysis.setAreasOfConcern(new ArrayList<>());
                
                return analysis;
            }
            
            throw new RuntimeException("No valid JSON found in Gemini response");
            
        } catch (Exception e) {
            log.error("Error parsing JSON from Gemini text: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Gemini JSON", e);
        }
    }

    private GeminiAnalysis createFallbackAnalysis(AnalysisResult result) {
        GeminiAnalysis fallback = new GeminiAnalysis();
        fallback.setExecutiveSummary("Analysis completed using technical forensic methods. Gemini AI analysis unavailable.");
        fallback.setKeyFindings(List.of(
            "Technical analysis completed successfully",
            "Multiple forensic techniques applied",
            "Results based on established forensic methods"
        ));
        fallback.setRiskAssessment(String.format(
            "Based on ensemble analysis with score %.1f/100, the document shows %s risk of manipulation.",
            result.getEnsembleScore().getFinalScore(),
            result.getEnsembleScore().getRiskLevel().toLowerCase()
        ));
        fallback.setGeminiConfidence(0.7);
        fallback.setRecommendations(List.of(
            "Review technical analysis results",
            "Consider additional forensic examination",
            "Verify source authenticity"
        ));
        // areasOfConcern will be populated from flaggedRegions in DocumentAnalysisService
        
        return fallback;
    }
}