# Document Manipulation Detection System

## Overview
This module extends the DeepGuard system with comprehensive document manipulation detection capabilities using multiple forensic techniques and AI analysis.

## Features

### Multi-Technique Analysis
- **Metadata Analysis (20% weight)**: EXIF/IPTC extraction, date inconsistencies, editing software detection
- **Forensics Analysis (40% weight)**: Error Level Analysis (ELA), copy-move detection, compression artifacts
- **Visual Anomaly Detection (10% weight)**: Basic file validation and anomaly checks
- **AI Model Integration (30% weight)**: Uses existing DeepSeek AI service for deepfake detection

### Ensemble Scoring
- Weighted combination of all analysis modules
- Risk levels: LOW (<50), MEDIUM (50-75), HIGH (>75)
- Confidence scoring based on module agreement

## API Endpoints

### Upload Document for Analysis
```http
POST /api/v1/document-analysis/analyze
Content-Type: multipart/form-data

file: [PDF or Image file]
```

**Response:**
```json
{
  "success": true,
  "message": "Document uploaded for analysis",
  "data": {
    "analysisId": "uuid",
    "status": "PROCESSING",
    "uploadTimestamp": "2024-01-01T12:00:00",
    "filename": "document.pdf"
  }
}
```

### Get Analysis Results
```http
GET /api/v1/document-analysis/results/{analysisId}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "document": {
      "id": "uuid",
      "filename": "document.pdf",
      "uploadDate": "2024-01-01T12:00:00"
    },
    "scores": {
      "metadata": {"score": 25.0},
      "forensics": {"score": 60.0},
      "visual": {"score": 10.0},
      "ai": {"score": 45.0},
      "final": {
        "totalScore": 42.5,
        "riskLevel": "LOW"
      }
    },
    "isManipulated": false,
    "processingTimeMs": 5000
  }
}
```

### Get Analysis History
```http
GET /api/v1/document-analysis/history?page=0&size=10
```

### Get Statistics
```http
GET /api/v1/document-analysis/stats
```

## Configuration

Add to your `application.yml`:

```yaml
app:
  document-analysis:
    ensemble:
      weights:
        metadata: 0.2
        forensics: 0.4
        visual: 0.1
        ai: 0.3
```

## Dependencies Added

```xml
<!-- OpenCV for image forensics -->
<dependency>
    <groupId>org.openpnp</groupId>
    <artifactId>opencv</artifactId>
    <version>4.9.0-0</version>
</dependency>

<!-- Apache PDFBox for PDF processing -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
</dependency>

<!-- Metadata extractor -->
<dependency>
    <groupId>com.drewnoakes</groupId>
    <artifactId>metadata-extractor</artifactId>
    <version>2.19.0</version>
</dependency>

<!-- TensorFlow Java -->
<dependency>
    <groupId>org.tensorflow</groupId>
    <artifactId>tensorflow-core-platform</artifactId>
    <version>0.5.0</version>
</dependency>
```

## Database Schema

The `DocumentAnalysis` entity is automatically created with:
- Individual module scores (metadata, forensics, visual, AI)
- Final ensemble score and risk level
- Processing time and forensic details
- Relationship to existing `MediaFile` entity

## Usage Example

1. Upload a document via the `/analyze` endpoint
2. Receive an `analysisId` for tracking
3. Poll the `/results/{analysisId}` endpoint for completion
4. Review detailed analysis breakdown including:
   - Individual module scores and weights
   - Final ensemble score
   - Risk level determination
   - Detailed forensic findings

## Integration with Existing System

This module integrates seamlessly with your existing:
- Authentication system (uses `UserPrincipal`)
- File storage system (extends `MediaFile`)
- AI services (leverages `DeepSeekAIService`)
- Database configuration (PostgreSQL)
- Security configuration

The system maintains your existing architecture patterns while adding comprehensive document manipulation detection capabilities.