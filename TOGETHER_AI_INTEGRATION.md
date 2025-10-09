# Together AI Integration Guide

## Overview
This DeepGuard Backend uses Together AI instead of TensorFlow Java/ONNX Runtime for superior AI capabilities.

## Benefits Over Traditional Approach

### 1. **No Local Model Management**
- No need to download/store large model files
- No GPU resource management
- No model versioning complexity

### 2. **Advanced Models**
- **Deepfake Detection**: Llama Vision models for image/video analysis
- **Breach Detection**: Meta-Llama language models for text analysis
- Always access to latest model versions

### 3. **Scalability**
- Automatic scaling based on demand
- No infrastructure bottlenecks
- Professional-grade inference infrastructure

### 4. **Cost Efficiency**
- Pay-per-use pricing model
- No upfront infrastructure costs
- Optimal resource utilization

## Implementation Details

### Configuration
```yaml
app:
  ai:
    together:
      api-key: ${TOGETHER_AI_API_KEY}
      base-url: https://api.together.xyz/v1
      timeout: 300000
      max-concurrent: 5
```

### Key Services

#### TogetherAIService
- `analyzeImageForDeepfake()` - Uses Llama Vision for media analysis
- `analyzeTextForBreaches()` - Uses Meta-Llama for breach detection
- Combines regex patterns with AI for enhanced accuracy

#### Integration Points
1. **DeepfakeController** → TogetherAIService → Llama Vision
2. **BreachController** → AnalyzerService → TogetherAIService → Meta-Llama

## API Usage Examples

### Deepfake Analysis
```bash
curl -X POST http://localhost:8080/api/v1/deepfake/analyze \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@suspicious_image.jpg"
```

### Breach Detection
```bash
curl -X POST http://localhost:8080/api/v1/breach/scan \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "My email is john@example.com and SSN is 123-45-6789"}'
```

## Environment Setup

1. **Get Together AI API Key**
   ```bash
   export TOGETHER_AI_API_KEY="your-api-key-here"
   ```

2. **Configure Application**
   - Update `application.yml` with your API key
   - Adjust timeout and concurrency settings as needed

3. **Test Integration**
   ```bash
   mvn test -Dtest=TogetherAIServiceTest
   ```

## Monitoring & Observability

- **Metrics**: Spring Actuator endpoints for AI service metrics
- **Logging**: Structured logging for AI requests/responses
- **Circuit Breaker**: Resilience4j for fault tolerance
- **Rate Limiting**: Built-in rate limiting for API calls

## Production Considerations

1. **API Key Security**: Store in environment variables or AWS Secrets Manager
2. **Rate Limiting**: Configure based on Together AI plan limits
3. **Caching**: Implement result caching for repeated analyses
4. **Monitoring**: Set up alerts for API failures or high latency
5. **Cost Monitoring**: Track API usage and costs

## Comparison with Traditional Approach

| Aspect | Together AI | TensorFlow Java |
|--------|-------------|-----------------|
| Setup Complexity | Low | High |
| Infrastructure | None | GPU servers |
| Model Updates | Automatic | Manual |
| Scalability | Automatic | Manual |
| Cost Model | Pay-per-use | Fixed infrastructure |
| Maintenance | Minimal | High |
| Performance | Professional-grade | Depends on hardware |

## Conclusion

Together AI integration provides a modern, scalable, and cost-effective solution for AI-powered deepfake detection and breach monitoring, eliminating the complexity of traditional model deployment while providing superior capabilities.