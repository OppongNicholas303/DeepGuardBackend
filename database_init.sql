-- Database initialization script for Document Manipulation Detection System

CREATE DATABASE IF NOT EXISTS doc_analysis;
USE doc_analysis;

-- Documents table
CREATE TABLE documents (
  id VARCHAR(36) PRIMARY KEY,
  filename VARCHAR(255),
  file_path VARCHAR(500),
  file_hash VARCHAR(64),
  upload_date TIMESTAMP,
  file_size BIGINT,
  file_type VARCHAR(20)
);

-- Analysis results table
CREATE TABLE analysis_results (
  id VARCHAR(36) PRIMARY KEY,
  document_id VARCHAR(36),
  metadata_score DECIMAL(5,2),
  forensics_score DECIMAL(5,2),
  visual_score DECIMAL(5,2),
  ai_score DECIMAL(5,2),
  final_score DECIMAL(5,2),
  risk_level VARCHAR(20),
  is_manipulated BOOLEAN,
  forensic_details JSON,
  analysis_timestamp TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES documents(id)
);