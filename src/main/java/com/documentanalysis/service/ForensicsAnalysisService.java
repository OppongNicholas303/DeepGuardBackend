package com.documentanalysis.service;

import com.documentanalysis.model.ModuleScore;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Service
@Slf4j
public class ForensicsAnalysisService {

    static {
        nu.pattern.OpenCV.loadShared();
    }

    public ModuleScore analyze(File file) {
        List<String> findings = new ArrayList<>();
        
        try {
            String fileName = file.getName().toLowerCase();
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".png") && !fileName.endsWith(".jpeg")) {
                ModuleScore result = new ModuleScore();
                result.setScore(0);
                result.setConfidence(0.5);
                result.setFindings(List.of("Non-image file - forensics analysis not applicable"));
                return result;
            }
            
            Mat image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
            
            int totalScore = 0;
            
            // Error Level Analysis
            double elaScore = performELA(image);
            if (elaScore > 0.3) {
                findings.add("ELA detected manipulation regions");
                totalScore += 40;
            }
            
            // Copy-move detection
            boolean copyMoveDetected = detectCopyMove(image);
            if (copyMoveDetected) {
                findings.add("Copy-move forgery detected");
                totalScore += 35;
            }
            
            // Compression analysis
            double compressionScore = analyzeCompression(file);
            if (compressionScore > 0.4) {
                findings.add("Compression artifacts detected");
                totalScore += 25;
            }
            
            ModuleScore result = new ModuleScore();
            result.setScore(Math.min(totalScore, 100));
            result.setConfidence(0.85);
            result.setFindings(findings);
            return result;
            
        } catch (Exception e) {
            log.error("Forensics analysis error: {}", e.getMessage());
            ModuleScore result = new ModuleScore();
            result.setScore(50);
            result.setConfidence(0.5);
            result.setFindings(List.of("Error during forensics analysis"));
            return result;
        }
    }

    private double performELA(Mat image) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat blurred = new Mat();
            Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
            
            Mat diff = new Mat();
            Core.absdiff(gray, blurred, diff);
            
            Scalar meanDiff = Core.mean(diff);
            return meanDiff.val[0] / 255.0;
            
        } catch (Exception e) {
            return 0.0;
        }
    }

    private boolean detectCopyMove(Mat image) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            ORB orb = ORB.create(500);
            MatOfKeyPoint keypoints = new MatOfKeyPoint();
            Mat descriptors = new Mat();
            
            orb.detectAndCompute(gray, new Mat(), keypoints, descriptors);
            
            return keypoints.toArray().length > 100; // Simplified detection
            
        } catch (Exception e) {
            return false;
        }
    }

    private double analyzeCompression(File file) {
        try {
            BufferedImage image = ImageIO.read(new FileInputStream(file));
            
            int blockSize = 8;
            double totalVariation = 0;
            int blockCount = 0;
            
            for (int y = 0; y < image.getHeight() - blockSize; y += blockSize) {
                for (int x = 0; x < image.getWidth() - blockSize; x += blockSize) {
                    double variation = calculateBlockVariation(image, x, y, blockSize);
                    totalVariation += variation;
                    blockCount++;
                }
            }
            
            return blockCount > 0 ? totalVariation / blockCount : 0.0;
            
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculateBlockVariation(BufferedImage image, int startX, int startY, int blockSize) {
        double sum = 0;
        double sumSquares = 0;
        int count = 0;
        
        for (int y = startY; y < startY + blockSize && y < image.getHeight(); y++) {
            for (int x = startX; x < startX + blockSize && x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                
                sum += gray;
                sumSquares += gray * gray;
                count++;
            }
        }
        
        if (count == 0) return 0;
        
        double mean = sum / count;
        double variance = (sumSquares / count) - (mean * mean);
        return Math.sqrt(variance) / 255.0;
    }
}