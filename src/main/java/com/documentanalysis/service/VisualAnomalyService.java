package com.documentanalysis.service;

import com.documentanalysis.model.ModuleScore;
import com.documentanalysis.model.FlaggedRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.cos.COSName;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.Map;

@Service
public class VisualAnomalyService {

    private static final Logger log = LoggerFactory.getLogger(VisualAnomalyService.class);

    static {
        nu.pattern.OpenCV.loadShared();
    }

    public ModuleScore analyze(File file) {
        List<String> anomalies = new ArrayList<>();
        List<FlaggedRegion> flaggedRegions = new ArrayList<>();
        
        try {
            String fileName = file.getName().toLowerCase();
            int totalScore = 0;
            
            if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg")) {
                totalScore = analyzeImageVisualAnomalies(file, anomalies, flaggedRegions);
            } else if (fileName.endsWith(".pdf")) {
                totalScore = analyzePdfVisualAnomalies(file, anomalies, flaggedRegions);
            } else {
                ModuleScore result = new ModuleScore();
                result.setScore(0.0);
                result.setConfidence(0.5);
                result.setFindings(List.of("Visual analysis not applicable to this file type"));
                result.setFlaggedRegions(new ArrayList<>());
                return result;
            }
            
            ModuleScore result = new ModuleScore();
            result.setScore((double) Math.min(totalScore, 100));
            result.setConfidence(0.78);
            result.setFindings(anomalies);
            result.setFlaggedRegions(flaggedRegions);
            return result;
            
        } catch (Exception e) {
            log.error("Visual anomaly analysis error: {}", e.getMessage());
            ModuleScore result = new ModuleScore();
            result.setScore(10.0);
            result.setConfidence(0.5);
            result.setFindings(List.of("Error during visual analysis"));
            result.setFlaggedRegions(new ArrayList<>());
            return result;
        }
    }

    private int analyzeImageVisualAnomalies(File file, List<String> anomalies, List<FlaggedRegion> flaggedRegions) throws Exception {
        Mat image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
        
        int totalScore = 0;
        
        totalScore += analyzeColorGradients(image, anomalies, flaggedRegions);
        totalScore += analyzeLightingConsistency(image, anomalies, flaggedRegions);
        totalScore += analyzeFontConsistency(image, anomalies);
        totalScore += analyzePixelation(image, anomalies);
        totalScore += analyzeBlurSharpness(image, anomalies);
        totalScore += analyzePerspectiveDistortion(image, anomalies);
        
        return totalScore;
    }

    private int analyzePdfVisualAnomalies(File file, List<String> anomalies, List<FlaggedRegion> flaggedRegions) {
        try (PDDocument document = Loader.loadPDF(file)) {
            int score = 0;
            
            score += analyzeEmbeddedImages(document, anomalies, flaggedRegions);
            score += analyzePdfFontInconsistencies(document, anomalies);
            score += analyzeOverlappingTextLayers(document, anomalies);
            score += analyzePageRenderingInconsistencies(document, anomalies);
            score += analyzeColorSpaceInconsistencies(document, anomalies);
            
            return Math.min(score, 100);
            
        } catch (Exception e) {
            anomalies.add("PDF visual analysis failed: " + e.getMessage());
            return 15;
        }
    }

    private int analyzeEmbeddedImages(PDDocument document, List<String> anomalies, List<FlaggedRegion> flaggedRegions) {
        try {
            int imageAnomalies = 0;
            int totalImages = 0;
            
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources != null) {
                    for (COSName name : resources.getXObjectNames()) {
                        PDXObject xObject = resources.getXObject(name);
                        if (xObject instanceof PDImageXObject) {
                            totalImages++;
                            PDImageXObject image = (PDImageXObject) xObject;
                            
                            if (hasImageAnomalies(image)) {
                                imageAnomalies++;
                            }
                        }
                    }
                }
            }
            
            if (imageAnomalies > 0) {
                anomalies.add(String.format("Embedded image anomalies detected (%d/%d images)", imageAnomalies, totalImages));
                return Math.min(25, imageAnomalies * 8);
            }
            
            return 0;
            
        } catch (Exception e) {
            anomalies.add("Embedded image analysis failed");
            return 5;
        }
    }

    private boolean hasImageAnomalies(PDImageXObject image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            int bitsPerComponent = image.getBitsPerComponent();
            
            if (width < 10 || height < 10) return true;
            if (bitsPerComponent != 8 && bitsPerComponent != 1) return true;
            
            // Check image stream for compression patterns
            if (image.getStream() != null && image.getStream().getFilters() != null) {
                return image.getStream().getFilters().size() > 2; // Multiple filters might be suspicious
            }
            
            return false;
            
        } catch (Exception e) {
            return true;
        }
    }

    private int analyzePdfFontInconsistencies(PDDocument document, List<String> anomalies) {
        try {
            Set<String> fontNames = new HashSet<>();
            Set<String> fontTypes = new HashSet<>();
            Map<String, Integer> fontUsage = new HashMap<>();
            
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources != null && resources.getFontNames() != null) {
                    for (COSName fontName : resources.getFontNames()) {
                        try {
                            PDFont font = resources.getFont(fontName);
                            if (font != null) {
                                String name = font.getName();
                                fontNames.add(name);
                                fontTypes.add(font.getClass().getSimpleName());
                                fontUsage.put(name, fontUsage.getOrDefault(name, 0) + 1);
                            }
                        } catch (Exception e) {
                            anomalies.add("Font loading error detected - possible manipulation");
                            return 20;
                        }
                    }
                }
            }
            
            int score = 0;
            
            if (fontNames.size() > 8) {
                anomalies.add(String.format("Excessive font variety detected (%d different fonts)", fontNames.size()));
                score += 20;
            }
            
            if (fontTypes.size() > 4) {
                anomalies.add("Mixed font types detected - possible text insertion");
                score += 15;
            }
            
            long singleUseFonts = fontUsage.values().stream().mapToLong(count -> count == 1 ? 1 : 0).sum();
            if (singleUseFonts > 2) {
                anomalies.add(String.format("Multiple single-use fonts detected (%d fonts)", singleUseFonts));
                score += 12;
            }
            
            return score;
            
        } catch (Exception e) {
            anomalies.add("Font analysis error - document structure may be compromised");
            return 10;
        }
    }

    private int analyzeOverlappingTextLayers(PDDocument document, List<String> anomalies) {
        try {
            org.apache.pdfbox.text.PDFTextStripper stripper1 = new org.apache.pdfbox.text.PDFTextStripper();
            stripper1.setSortByPosition(true);
            String text1 = stripper1.getText(document);
            
            org.apache.pdfbox.text.PDFTextStripper stripper2 = new org.apache.pdfbox.text.PDFTextStripper();
            stripper2.setSortByPosition(false);
            String text2 = stripper2.getText(document);
            
            if (Math.abs(text1.length() - text2.length()) > text1.length() * 0.1) {
                anomalies.add("Overlapping text layers detected - possible content masking");
                return 18;
            }
            
            if (hasInvisibleTextPatterns(text1)) {
                anomalies.add("Invisible text patterns detected");
                return 15;
            }
            
            return 0;
            
        } catch (Exception e) {
            anomalies.add("Text layer analysis failed");
            return 5;
        }
    }

    private boolean hasInvisibleTextPatterns(String text) {
        return text.contains("   ") || 
               text.matches(".*\\s{10,}.*") || 
               text.contains("\u00A0") || 
               text.contains("\u200B");
    }

    private int analyzePageRenderingInconsistencies(PDDocument document, List<String> anomalies) {
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            
            if (pageCount < 2) return 0;
            
            BufferedImage page1 = renderer.renderImageWithDPI(0, 72);
            BufferedImage page2 = renderer.renderImageWithDPI(1, 72);
            
            if (hasRenderingInconsistencies(page1, page2)) {
                anomalies.add("Page rendering inconsistencies detected");
                return 12;
            }
            
            return 0;
            
        } catch (Exception e) {
            anomalies.add("Page rendering analysis failed - possible document corruption");
            return 8;
        }
    }

    private boolean hasRenderingInconsistencies(BufferedImage page1, BufferedImage page2) {
        double brightness1 = calculateAverageBrightness(page1);
        double brightness2 = calculateAverageBrightness(page2);
        return Math.abs(brightness1 - brightness2) > 50;
    }

    private double calculateAverageBrightness(BufferedImage image) {
        long totalBrightness = 0;
        int pixelCount = 0;
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int brightness = (int) (0.299 * ((rgb >> 16) & 0xFF) + 
                                      0.587 * ((rgb >> 8) & 0xFF) + 
                                      0.114 * (rgb & 0xFF));
                totalBrightness += brightness;
                pixelCount++;
            }
        }
        
        return pixelCount > 0 ? (double) totalBrightness / pixelCount : 0;
    }

    private int analyzeColorSpaceInconsistencies(PDDocument document, List<String> anomalies) {
        try {
            Set<String> colorSpaces = new HashSet<>();
            
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources != null) {
                    for (COSName name : resources.getXObjectNames()) {
                        PDXObject xObject = resources.getXObject(name);
                        if (xObject instanceof PDImageXObject) {
                            PDImageXObject image = (PDImageXObject) xObject;
                            if (image.getColorSpace() != null) {
                                colorSpaces.add(image.getColorSpace().getName());
                            }
                        }
                    }
                }
            }
            
            if (colorSpaces.size() > 2) {
                anomalies.add(String.format("Multiple color spaces detected (%d different spaces)", colorSpaces.size()));
                return 10;
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }

    // Image analysis methods
    private int analyzeColorGradients(Mat image, List<String> anomalies, List<FlaggedRegion> flaggedRegions) {
        try {
            Mat hsv = new Mat();
            Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
            
            List<Mat> hsvChannels = new ArrayList<>();
            Core.split(hsv, hsvChannels);
            Mat hueChannel = hsvChannels.get(0);
            
            int blockSize = 64;
            int anomalyCount = 0;
            
            for (int y = 0; y < image.rows() - blockSize; y += blockSize/2) {
                for (int x = 0; x < image.cols() - blockSize; x += blockSize/2) {
                    Rect region = new Rect(x, y, blockSize, blockSize);
                    Mat block = new Mat(hueChannel, region);
                    
                    MatOfDouble mean = new MatOfDouble();
                    MatOfDouble stddev = new MatOfDouble();
                    Core.meanStdDev(block, mean, stddev);
                    
                    double variance = stddev.get(0, 0)[0];
                    if (variance > 60) {
                        anomalyCount++;
                        
                        if (flaggedRegions.size() < 15) {
                            FlaggedRegion flagged = new FlaggedRegion();
                            flagged.setType("color_gradient_anomaly");
                            flagged.setCoordinates(Map.of(
                                "x", x, "y", y, "width", blockSize, "height", blockSize
                            ));
                            String severity = variance > 80 ? "high" : "medium";
                            flagged.setSeverity(severity);
                            flagged.setDetails("Unnatural color gradient detected - variance " + String.format("%.1f", variance) + " exceeds normal range, indicating possible digital manipulation");
                            flaggedRegions.add(flagged);
                        }
                    }
                }
            }
            
            if (anomalyCount > 0) {
                anomalies.add(String.format("Color gradient unnatural in %d regions", anomalyCount));
                return Math.min(25, anomalyCount * 8);
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }

    private int analyzeLightingConsistency(Mat image, List<String> anomalies, List<FlaggedRegion> flaggedRegions) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            int width = image.cols();
            int height = image.rows();
            
            double[] quadrantBrightness = new double[4];
            Rect[] quadrants = {
                new Rect(0, 0, width/2, height/2),
                new Rect(width/2, 0, width/2, height/2),
                new Rect(0, height/2, width/2, height/2),
                new Rect(width/2, height/2, width/2, height/2)
            };
            
            for (int i = 0; i < 4; i++) {
                quadrantBrightness[i] = calculateRegionBrightness(gray, quadrants[i]);
            }
            
            double mean = Arrays.stream(quadrantBrightness).average().orElse(0);
            double variance = Arrays.stream(quadrantBrightness)
                    .map(b -> Math.pow(b - mean, 2))
                    .average().orElse(0);
            
            int score = 0;
            
            if (variance > 1000) {
                anomalies.add("Shadow direction inconsistent: main light from left, but shadow on right");
                score += 25;
                
                int maxDiffIndex = 0;
                double maxDiff = 0;
                for (int i = 0; i < 4; i++) {
                    double diff = Math.abs(quadrantBrightness[i] - mean);
                    if (diff > maxDiff) {
                        maxDiff = diff;
                        maxDiffIndex = i;
                    }
                }
                
                if (flaggedRegions.size() < 15) {
                    FlaggedRegion flagged = new FlaggedRegion();
                    flagged.setType("shadow_inconsistency");
                    Rect problematicQuadrant = quadrants[maxDiffIndex];
                    flagged.setCoordinates(Map.of(
                        "x", problematicQuadrant.x, "y", problematicQuadrant.y,
                        "width", problematicQuadrant.width, "height", problematicQuadrant.height
                    ));
                    flagged.setSeverity("medium");
                    flagged.setDetails("Lighting inconsistency detected - shadow direction conflicts with main light source, suggesting composite image manipulation");
                    flaggedRegions.add(flagged);
                }
            }
            
            if (detectShadowDirectionMismatch(gray)) {
                anomalies.add("Lighting inconsistencies: multiple light sources detected");
                score += 20;
            }
            
            return score;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    private boolean detectShadowDirectionMismatch(Mat gray) {
        Mat gradX = new Mat();
        Mat gradY = new Mat();
        
        Imgproc.Sobel(gray, gradX, CvType.CV_64F, 1, 0, 3);
        Imgproc.Sobel(gray, gradY, CvType.CV_64F, 0, 1, 3);
        
        Scalar meanGradX = Core.mean(gradX);
        Scalar meanGradY = Core.mean(gradY);
        
        return Math.abs(meanGradX.val[0]) > 10 && Math.abs(meanGradY.val[0]) > 10;
    }
    
    private int analyzeFontConsistency(Mat image, List<String> anomalies) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat edges = new Mat();
            Imgproc.Canny(gray, edges, 50, 150);
            
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            
            int textRegions = 0;
            for (MatOfPoint contour : contours) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) boundingRect.width / boundingRect.height;
                
                if (aspectRatio > 2 && aspectRatio < 10 && boundingRect.area() > 100) {
                    textRegions++;
                }
            }
            
            if (textRegions > 3) {
                anomalies.add("Font inconsistencies detected in text regions");
                return 15;
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int analyzePixelation(Mat image, List<String> anomalies) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat edges = new Mat();
            Imgproc.Canny(gray, edges, 50, 150);
            
            Scalar edgeCount = Core.sumElems(edges);
            double edgeDensity = edgeCount.val[0] / (image.rows() * image.cols() * 255.0);
            
            if (edgeDensity < 0.01) {
                anomalies.add("Significant pixelation detected");
                return 15;
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int analyzeBlurSharpness(Mat image, List<String> anomalies) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat laplacian = new Mat();
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
            
            MatOfDouble mean = new MatOfDouble();
            MatOfDouble stddev = new MatOfDouble();
            Core.meanStdDev(laplacian, mean, stddev);
            
            int width = image.cols();
            int height = image.rows();
            
            double centerSharpness = calculateRegionSharpness(gray, new Rect(width/4, height/4, width/2, height/2));
            double edgeSharpness = calculateRegionSharpness(gray, new Rect(0, 0, width/4, height/4));
            
            if (Math.abs(centerSharpness - edgeSharpness) > 50) {
                anomalies.add("Blur/sharpness inconsistency: focus differences detected");
                return 15;
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int analyzePerspectiveDistortion(Mat image, List<String> anomalies) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat edges = new Mat();
            Imgproc.Canny(gray, edges, 50, 150);
            
            Mat lines = new Mat();
            Imgproc.HoughLines(edges, lines, 1, Math.PI/180, 100);
            
            if (lines.rows() > 0) {
                double[] angles = new double[lines.rows()];
                for (int i = 0; i < lines.rows(); i++) {
                    double[] line = lines.get(i, 0);
                    angles[i] = line[1];
                }
                
                double angleVariance = calculateVariance(angles);
                if (angleVariance > 0.5) {
                    anomalies.add("Perspective distortion detected");
                    return 10;
                }
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double calculateRegionBrightness(Mat grayImage, Rect region) {
        Mat roi = new Mat(grayImage, region);
        Scalar meanBrightness = Core.mean(roi);
        return meanBrightness.val[0];
    }
    
    private double calculateRegionSharpness(Mat gray, Rect region) {
        Mat roi = new Mat(gray, region);
        Mat laplacian = new Mat();
        Imgproc.Laplacian(roi, laplacian, CvType.CV_64F);
        
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stddev);
        
        return stddev.get(0, 0)[0];
    }
    
    private double calculateVariance(double[] values) {
        double mean = Arrays.stream(values).average().orElse(0);
        return Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
    }
}