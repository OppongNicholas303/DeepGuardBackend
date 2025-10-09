package DeepQuard.DeepGuardBackend.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
public class TogetherAIService {
    
    private static final Logger logger = LoggerFactory.getLogger(TogetherAIService.class);
    
    @Value("${app.ai.together.api-key}")
    private String apiKey;
    
    @Value("${app.ai.together.base-url:https://api.together.xyz/v1}")
    private String baseUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Regex patterns for breach detection
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}-?\\d{3}-?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-?\\d{2}-?\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api[_-]?key|token|secret)[\\s=:]+['\"]?([a-zA-Z0-9_-]{20,})['\"]?");
    
    @Async
    public CompletableFuture<Map<String, Object>> analyzeImageForDeepfake(MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
                
                HttpHeaders headers = createHeaders();
                Map<String, Object> request = Map.of(
                    "model", "meta-llama/Llama-Vision-Free",
                    "messages", new Object[]{
                        Map.of(
                            "role", "user",
                            "content", new Object[]{
                                Map.of("type", "text", "text", 
                                    "Analyze this image for deepfake artifacts. Look for inconsistencies in lighting, shadows, facial features, and digital manipulation signs. " +
                                    "Respond with JSON format: {\"isDeepfake\": boolean, \"confidence\": 0.0-1.0, \"anomalies\": [\"list of detected issues\"], \"riskLevel\": \"LOW/MEDIUM/HIGH\"}"),
                                Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image))
                            }
                        )
                    },
                    "max_tokens", 500,
                    "temperature", 0.1
                );
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/chat/completions", entity, Map.class);
                
                return parseDeepfakeResponse(response.getBody());
                
            } catch (Exception e) {
                logger.error("Error analyzing image for deepfake: {}", e.getMessage(), e);
                return createErrorResponse("Image analysis failed: " + e.getMessage());
            }
        });
    }
    
    @Async
    public CompletableFuture<Map<String, Object>> analyzeTextForBreaches(String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First, use regex patterns for quick detection
                Map<String, Object> regexResult = performRegexAnalysis(content);
                
                // Then enhance with AI analysis
                HttpHeaders headers = createHeaders();
                Map<String, Object> request = Map.of(
                    "model", "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo",
                    "messages", new Object[]{
                        Map.of("role", "user", "content", 
                            "Analyze this text for sensitive data breaches including PII, credentials, API keys, financial data. " +
                            "Respond with JSON format: {\"breachDetected\": boolean, \"detectedTypes\": [\"PII\", \"CREDENTIALS\", etc.], \"riskLevel\": \"LOW/MEDIUM/HIGH/CRITICAL\", \"confidence\": 0.0-1.0, \"snippet\": \"relevant text excerpt\"}\n\n" +
                            "Text to analyze: " + content.substring(0, Math.min(content.length(), 1000)))
                    },
                    "max_tokens", 300,
                    "temperature", 0.1
                );
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/chat/completions", entity, Map.class);
                
                Map<String, Object> aiResult = parseBreachResponse(response.getBody());
                
                // Combine regex and AI results
                return combineBreachResults(regexResult, aiResult);
                
            } catch (Exception e) {
                logger.error("Error analyzing text for breaches: {}", e.getMessage(), e);
                return createBreachErrorResponse("Text analysis failed: " + e.getMessage());
            }
        });
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }
    
    private Map<String, Object> parseDeepfakeResponse(Map<String, Object> response) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (response != null && response.containsKey("choices")) {
                List<?> choices = (List<?>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    String content = (String) message.get("content");
                    
                    // Try to parse JSON from AI response
                    try {
                        JsonNode jsonNode = objectMapper.readTree(content);
                        result.put("isDeepfake", jsonNode.get("isDeepfake").asBoolean(false));
                        result.put("confidence", new BigDecimal(jsonNode.get("confidence").asDouble(0.5)));
                        result.put("riskLevel", jsonNode.get("riskLevel").asText("MEDIUM"));
                        
                        List<String> anomalies = new ArrayList<>();
                        if (jsonNode.has("anomalies")) {
                            jsonNode.get("anomalies").forEach(node -> anomalies.add(node.asText()));
                        }
                        result.put("anomalies", anomalies);
                        
                    } catch (Exception e) {
                        // Fallback parsing
                        result.put("isDeepfake", content.toLowerCase().contains("deepfake") || content.toLowerCase().contains("fake"));
                        result.put("confidence", new BigDecimal("0.7"));
                        result.put("riskLevel", "MEDIUM");
                        result.put("anomalies", Arrays.asList("AI analysis completed"));
                    }
                    
                    result.put("rawResponse", content);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing deepfake response: {}", e.getMessage());
            return createErrorResponse("Failed to parse AI response");
        }
    }
    
    private Map<String, Object> parseBreachResponse(Map<String, Object> response) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (response != null && response.containsKey("choices")) {
                List<?> choices = (List<?>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    String content = (String) message.get("content");
                    
                    try {
                        JsonNode jsonNode = objectMapper.readTree(content);
                        result.put("breachDetected", jsonNode.get("breachDetected").asBoolean(false));
                        result.put("confidence", new BigDecimal(jsonNode.get("confidence").asDouble(0.5)));
                        result.put("riskLevel", jsonNode.get("riskLevel").asText("LOW"));
                        result.put("snippet", jsonNode.get("snippet").asText(""));
                        
                        List<String> detectedTypes = new ArrayList<>();
                        if (jsonNode.has("detectedTypes")) {
                            jsonNode.get("detectedTypes").forEach(node -> detectedTypes.add(node.asText()));
                        }
                        result.put("detectedTypes", detectedTypes);
                        
                    } catch (Exception e) {
                        // Fallback parsing
                        result.put("breachDetected", content.toLowerCase().contains("breach") || content.toLowerCase().contains("sensitive"));
                        result.put("confidence", new BigDecimal("0.6"));
                        result.put("riskLevel", "MEDIUM");
                        result.put("detectedTypes", Arrays.asList("PII"));
                        result.put("snippet", content.substring(0, Math.min(content.length(), 100)));
                    }
                    
                    result.put("rawResponse", content);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing breach response: {}", e.getMessage());
            return createBreachErrorResponse("Failed to parse AI response");
        }
    }
    
    private Map<String, Object> performRegexAnalysis(String content) {
        Map<String, Object> result = new HashMap<>();
        List<String> detectedTypes = new ArrayList<>();
        List<String> findings = new ArrayList<>();
        
        if (EMAIL_PATTERN.matcher(content).find()) {
            detectedTypes.add("EMAIL");
            findings.add("Email addresses detected");
        }
        
        if (PHONE_PATTERN.matcher(content).find()) {
            detectedTypes.add("PII");
            findings.add("Phone numbers detected");
        }
        
        if (SSN_PATTERN.matcher(content).find()) {
            detectedTypes.add("SSN");
            findings.add("Social Security Numbers detected");
        }
        
        if (CREDIT_CARD_PATTERN.matcher(content).find()) {
            detectedTypes.add("CREDIT_CARD");
            findings.add("Credit card numbers detected");
        }
        
        if (API_KEY_PATTERN.matcher(content).find()) {
            detectedTypes.add("API_KEY");
            findings.add("API keys or tokens detected");
        }
        
        result.put("regexDetected", !detectedTypes.isEmpty());
        result.put("regexTypes", detectedTypes);
        result.put("regexFindings", findings);
        
        return result;
    }
    
    private Map<String, Object> combineBreachResults(Map<String, Object> regexResult, Map<String, Object> aiResult) {
        Map<String, Object> combined = new HashMap<>();
        
        boolean regexDetected = (Boolean) regexResult.getOrDefault("regexDetected", false);
        boolean aiDetected = (Boolean) aiResult.getOrDefault("breachDetected", false);
        
        combined.put("breachDetected", regexDetected || aiDetected);
        
        // Combine detected types
        List<String> allTypes = new ArrayList<>();
        allTypes.addAll((List<String>) regexResult.getOrDefault("regexTypes", new ArrayList<>()));
        allTypes.addAll((List<String>) aiResult.getOrDefault("detectedTypes", new ArrayList<>()));
        combined.put("detectedTypes", allTypes.stream().distinct().toList());
        
        // Use higher confidence and risk level
        BigDecimal confidence = (BigDecimal) aiResult.getOrDefault("confidence", new BigDecimal("0.5"));
        if (regexDetected) {
            confidence = confidence.max(new BigDecimal("0.8")); // High confidence for regex matches
        }
        combined.put("confidence", confidence);
        
        String riskLevel = (String) aiResult.getOrDefault("riskLevel", "LOW");
        if (regexDetected && allTypes.contains("SSN") || allTypes.contains("CREDIT_CARD")) {
            riskLevel = "CRITICAL";
        } else if (regexDetected && (allTypes.contains("API_KEY") || allTypes.contains("CREDENTIALS"))) {
            riskLevel = "HIGH";
        }
        combined.put("riskLevel", riskLevel);
        
        combined.put("snippet", aiResult.getOrDefault("snippet", ""));
        combined.put("regexFindings", regexResult.getOrDefault("regexFindings", new ArrayList<>()));
        
        return combined;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("isDeepfake", false);
        error.put("confidence", new BigDecimal("0.0"));
        error.put("riskLevel", "UNKNOWN");
        error.put("error", message);
        return error;
    }
    
    private Map<String, Object> createBreachErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("breachDetected", false);
        error.put("confidence", new BigDecimal("0.0"));
        error.put("riskLevel", "UNKNOWN");
        error.put("detectedTypes", new ArrayList<>());
        error.put("error", message);
        return error;
    }
}