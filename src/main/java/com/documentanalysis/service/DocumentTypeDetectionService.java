package com.documentanalysis.service;

import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class DocumentTypeDetectionService {
    
    public enum DocumentType {
        CAMERA_PHOTO,      // Original camera photo
        SCREENSHOT,        // Screenshot/screen capture
        SCANNED_DOCUMENT,  // Scanned paper document
        DIGITAL_DOCUMENT,  // Born-digital document
        CERTIFICATE,       // Certificate/diploma
        ID_DOCUMENT,       // ID card/passport
        UNKNOWN
    }
    
    public DocumentType detectType(File file, String metadata) {
        String fileName = file.getName().toLowerCase();
        
        // Check for camera indicators
        if (metadata.contains("Camera") || metadata.contains("EXIF") || 
            metadata.contains("GPS") || metadata.contains("Flash")) {
            return DocumentType.CAMERA_PHOTO;
        }
        
        // Check for screenshot indicators
        if (fileName.contains("screenshot") || fileName.contains("screen") ||
            metadata.contains("Screenshot") || metadata.contains("Snipping")) {
            return DocumentType.SCREENSHOT;
        }
        
        // Check for certificate patterns
        if (fileName.contains("certificate") || fileName.contains("diploma") ||
            fileName.contains("cert") || metadata.contains("Certificate")) {
            return DocumentType.CERTIFICATE;
        }
        
        // Check for ID document patterns
        if (fileName.contains("passport") || fileName.contains("license") ||
            fileName.contains("id") || fileName.contains("card")) {
            return DocumentType.ID_DOCUMENT;
        }
        
        // Check for scan indicators
        if (fileName.contains("scan") || metadata.contains("Scanner") ||
            metadata.contains("DPI") && !metadata.contains("Camera")) {
            return DocumentType.SCANNED_DOCUMENT;
        }
        
        return DocumentType.DIGITAL_DOCUMENT;
    }
}