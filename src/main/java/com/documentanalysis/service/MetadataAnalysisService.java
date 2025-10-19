package com.documentanalysis.service;

import com.documentanalysis.model.ModuleScore;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
@Slf4j
public class MetadataAnalysisService {

    public ModuleScore analyze(File file) {
        List<String> findings = new ArrayList<>();
        int score = 0;
        
        try {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg")) {
                score = analyzeImageMetadata(file, findings);
            } else if (fileName.endsWith(".pdf")) {
                score = analyzePdfMetadata(file, findings);
            }
            
        } catch (Exception e) {
            log.error("Metadata analysis error: {}", e.getMessage());
            score = 50;
        }
        
        ModuleScore result = new ModuleScore();
        result.setScore(score);
        result.setConfidence(0.8);
        result.setFindings(findings);
        return result;
    }

    private int analyzeImageMetadata(File file, List<String> anomalies) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        
        int score = 0;
        
        if (exifDirectory == null) {
            anomalies.add("Missing EXIF data");
            return 40;
        }
        
        String software = exifDirectory.getString(ExifIFD0Directory.TAG_SOFTWARE);
        if (software != null && isEditingSoftware(software)) {
            anomalies.add("Editing software detected: " + software);
            score += 30;
        }
        
        Date creationDate = exifDirectory.getDate(ExifIFD0Directory.TAG_DATETIME);
        Date modificationDate = exifDirectory.getDate(ExifIFD0Directory.TAG_DATETIME_DIGITIZED);
        
        if (creationDate != null && modificationDate != null) {
            long timeDiff = Math.abs(creationDate.getTime() - modificationDate.getTime());
            if (timeDiff > 86400000) {
                anomalies.add("Date inconsistency detected");
                score += 25;
            }
        }
        
        return Math.min(score, 100);
    }

    private int analyzePdfMetadata(File file, List<String> anomalies) throws Exception {
        try (PDDocument document = PDDocument.load(file)) {
            PDDocumentInformation info = document.getDocumentInformation();
            
            int score = 0;
            String producer = info.getProducer();
            
            if (producer != null && isEditingSoftware(producer)) {
                anomalies.add("PDF editing software detected: " + producer);
                score += 30;
            }
            
            Calendar creationDate = info.getCreationDate();
            Calendar modificationDate = info.getModificationDate();
            
            if (creationDate != null && modificationDate != null) {
                long timeDiff = Math.abs(creationDate.getTimeInMillis() - modificationDate.getTimeInMillis());
                if (timeDiff > 86400000) {
                    anomalies.add("PDF date inconsistency");
                    score += 25;
                }
            }
            
            return Math.min(score, 100);
        }
    }

    private boolean isEditingSoftware(String software) {
        String[] editingSoftware = {
            "photoshop", "gimp", "paint.net", "canva", "pixlr",
            "acrobat", "foxit", "nitro", "pdfelement"
        };
        
        String softwareLower = software.toLowerCase();
        return Arrays.stream(editingSoftware).anyMatch(softwareLower::contains);
    }
}