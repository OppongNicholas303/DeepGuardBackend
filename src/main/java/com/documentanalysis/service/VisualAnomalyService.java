package com.documentanalysis.service;

import com.documentanalysis.model.ModuleScore;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
@Slf4j
public class VisualAnomalyService {

    static {
        nu.pattern.OpenCV.loadShared();
    }

    public ModuleScore analyze(File file) {
        List<String> anomalies = new ArrayList<>();
        
        try {
            String fileName = file.getName().toLowerCase();
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".png") && !fileName.endsWith(".jpeg")) {
                ModuleScore result = new ModuleScore();
                result.setScore(0);
                result.setConfidence(0.5);
                result.setFindings(List.of("Non-image file - visual analysis not applicable"));
                return result;
            }
            
            Mat image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
            
            int totalScore = 0;
            
            // Color gradient analysis
            totalScore += analyzeColorGradients(image, anomalies);
            
            // Lighting consistency
            totalScore += analyzeLightingConsistency(image, anomalies);
            
            // Pixelation detection
            totalScore += analyzePixelation(image, anomalies);
            
            // Edge detection for splicing
            totalScore += analyzeSplicingBoundaries(image, anomalies);
            
            ModuleScore result = new ModuleScore();
            result.setScore(Math.min(totalScore, 100));
            result.setConfidence(0.75);
            result.setFindings(anomalies);
            return result;
            
        } catch (Exception e) {
            log.error("Visual anomaly analysis error: {}", e.getMessage());
            ModuleScore result = new ModuleScore();
            result.setScore(10);
            result.setConfidence(0.5);
            result.setFindings(List.of("Error during visual analysis"));
            return result;
        }
    }

    private int analyzeColorGradients(Mat image, List<String> anomalies) {
        try {
            Mat hsv = new Mat();
            Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
            
            List<Mat> hsvChannels = new ArrayList<>();
            Core.split(hsv, hsvChannels);
            Mat hueChannel = hsvChannels.get(0);
            
            MatOfDouble mean = new MatOfDouble();
            MatOfDouble stddev = new MatOfDouble();
            Core.meanStdDev(hueChannel, mean, stddev);
            
            double variance = stddev.get(0, 0)[0];
            if (variance > 30) {
                anomalies.add("Inconsistent color gradients detected");
                return 25;
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }

    private int analyzeLightingConsistency(Mat image, List<String> anomalies) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            int width = image.cols();
            int height = image.rows();
            
            double[] quadrantBrightness = new double[4];
            quadrantBrightness[0] = calculateRegionBrightness(gray, new Rect(0, 0, width/2, height/2));
            quadrantBrightness[1] = calculateRegionBrightness(gray, new Rect(width/2, 0, width/2, height/2));
            quadrantBrightness[2] = calculateRegionBrightness(gray, new Rect(0, height/2, width/2, height/2));
            quadrantBrightness[3] = calculateRegionBrightness(gray, new Rect(width/2, height/2, width/2, height/2));
            
            double mean = Arrays.stream(quadrantBrightness).average().orElse(0);
            double variance = Arrays.stream(quadrantBrightness)
                    .map(b -> Math.pow(b - mean, 2))
                    .average().orElse(0);
            
            if (variance > 1000) {
                anomalies.add("Lighting inconsistency detected");
                return 20;
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

    private int analyzeSplicingBoundaries(Mat image, List<String> anomalies) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat edges = new Mat();
            Imgproc.Canny(gray, edges, 50, 150);
            
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            
            int suspiciousContours = 0;
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > 1000) {
                    suspiciousContours++;
                }
            }
            
            if (suspiciousContours > 5) {
                anomalies.add("Suspicious splicing boundaries detected");
                return 30;
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
}