package com.documentanalysis.repository;

import com.documentanalysis.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, String> {
}