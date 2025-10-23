package com.documentanalysis.service;

import com.documentanalysis.model.ModuleScore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class TextManipulationService {

    private static final Logger log = LoggerFactory.getLogger(TextManipulationService.class);

    public ModuleScore analyze(File file) {
        List<String> findings = new ArrayList<>();
        
        try {
            String fileName = file.getName().toLowerCase();
            if (!fileName.endsWith(".pdf")) {
                ModuleScore result = new ModuleScore();
                result.setScore(0.0);
                result.setConfidence(0.5);
                result.setFindings(List.of("Text manipulation analysis only applicable to PDF files"));
                return result;
            }
            
            int totalScore = analyzePdfText(file, findings);
            
            ModuleScore result = new ModuleScore();
            result.setScore((double) Math.min(totalScore, 100));
            result.setConfidence(0.82);
            result.setFindings(findings);
            return result;
            
        } catch (Exception e) {
            log.error("Text manipulation analysis error: {}", e.getMessage());
            ModuleScore result = new ModuleScore();
            result.setScore(25.0);
            result.setConfidence(0.5);
            result.setFindings(List.of("Error during text analysis: " + e.getMessage()));
            return result;
        }
    }

    private int analyzePdfText(File file, List<String> findings) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            int score = 0;
            
            // Font inconsistency analysis (+20 points)
            score += analyzeFontInconsistencies(document, findings);
            
            // Character encoding analysis (+25 points)
            score += analyzeCharacterEncodings(document, findings);
            
            // Text layer anomalies (+18 points)
            score += analyzeTextLayerAnomalies(document, findings);
            
            // Signature tampering detection (+30 points)
            score += analyzeSignatureTampering(document, findings);
            
            // Suspicious modifications (+15 points)
            score += analyzeSuspiciousModifications(document, findings);
            
            return score;
        }
    }

    private int analyzeFontInconsistencies(PDDocument document, List<String> findings) {
        try {
            Set<String> fontNames = new HashSet<>();
            Set<String> fontTypes = new HashSet<>();
            
            for (PDPage page : document.getPages()) {
                if (page.getResources() != null && page.getResources().getFontNames() != null) {
                    for (COSName fontName : page.getResources().getFontNames()) {
                        try {
                            PDFont font = page.getResources().getFont(fontName);
                            if (font != null) {
                                fontNames.add(font.getName());
                                fontTypes.add(font.getClass().getSimpleName());
                            }
                        } catch (Exception e) {
                            findings.add("Font loading error detected - possible manipulation");
                            return 20;
                        }
                    }
                }
            }
            
            if (fontNames.size() > 10) {
                findings.add(String.format("Excessive font variety detected (%d different fonts)", fontNames.size()));
                return 20;
            }
            
            if (fontTypes.size() > 3) {
                findings.add("Mixed font types detected - possible text insertion");
                return 15;
            }
            
            return 0;
            
        } catch (Exception e) {
            findings.add("Font analysis error - document structure may be compromised");
            return 10;
        }
    }

    private int analyzeCharacterEncodings(PDDocument document, List<String> findings) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            int score = 0;
            int unicodeCount = 0;
            int controlCharCount = 0;
            int nonPrintableCount = 0;
            
            for (char c : text.toCharArray()) {
                if (c > 127) unicodeCount++;
                if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') controlCharCount++;
                if (!Character.isDefined(c)) nonPrintableCount++;
            }
            
            double textLength = text.length();
            if (textLength > 0) {
                double unicodeRatio = unicodeCount / textLength;
                double controlRatio = controlCharCount / textLength;
                double nonPrintableRatio = nonPrintableCount / textLength;
                
                if (unicodeRatio > 0.3) {
                    findings.add("High Unicode character usage detected");
                    score += 10;
                }
                
                if (controlRatio > 0.05) {
                    findings.add("Unusual control characters detected");
                    score += 15;
                }
                
                if (nonPrintableRatio > 0.01) {
                    findings.add("Non-printable characters detected - possible hidden content");
                    score += 25;
                }
            }
            
            if (text.contains("\u200B") || text.contains("\u200C") || text.contains("\u200D")) {
                findings.add("Zero-width characters detected - possible text hiding");
                score += 25;
            }
            
            return score;
            
        } catch (Exception e) {
            findings.add("Character encoding analysis failed");
            return 5;
        }
    }

    private int analyzeTextLayerAnomalies(PDDocument document, List<String> findings) {
        try {
            int score = 0;
            int pageCount = document.getNumberOfPages();
            
            int emptyPages = 0;
            for (int i = 0; i < pageCount; i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(document);
                
                if (pageText.trim().isEmpty()) {
                    emptyPages++;
                }
            }
            
            if (emptyPages > 0 && emptyPages < pageCount) {
                findings.add(String.format("Mixed content pages detected (%d empty pages)", emptyPages));
                score += 15;
            }
            
            if (hasOverlappingTextLayers(document)) {
                findings.add("Overlapping text layers detected - possible content masking");
                score += 18;
            }
            
            return score;
            
        } catch (Exception e) {
            findings.add("Text layer analysis failed");
            return 5;
        }
    }

    private int analyzeSignatureTampering(PDDocument document, List<String> findings) {
        try {
            int score = 0;
            
            if (document.getSignatureDictionaries() != null && !document.getSignatureDictionaries().isEmpty()) {
                findings.add("Digital signatures present - verification recommended");
                
                if (document.getDocument().getXrefTable().size() > 1) {
                    findings.add("Document modified after digital signing - signature may be invalid");
                    score += 30;
                }
            }
            
            return score;
            
        } catch (Exception e) {
            findings.add("Signature analysis failed");
            return 5;
        }
    }

    private int analyzeSuspiciousModifications(PDDocument document, List<String> findings) {
        try {
            int score = 0;
            
            int updateCount = document.getDocument().getXrefTable().size();
            if (updateCount > 3) {
                findings.add(String.format("Multiple incremental updates detected (%d updates)", updateCount));
                score += 15;
            }
            
            if (document.getDocumentCatalog().getAcroForm() != null) {
                findings.add("Form fields present - potential tampering target");
                score += 10;
            }
            
            return score;
            
        } catch (Exception e) {
            findings.add("Modification analysis failed");
            return 5;
        }
    }

    private boolean hasOverlappingTextLayers(PDDocument document) {
        try {
            PDFTextStripper stripper1 = new PDFTextStripper();
            stripper1.setSortByPosition(true);
            String text1 = stripper1.getText(document);
            
            PDFTextStripper stripper2 = new PDFTextStripper();
            stripper2.setSortByPosition(false);
            String text2 = stripper2.getText(document);
            
            return Math.abs(text1.length() - text2.length()) > text1.length() * 0.1;
            
        } catch (Exception e) {
            return false;
        }
    }
}