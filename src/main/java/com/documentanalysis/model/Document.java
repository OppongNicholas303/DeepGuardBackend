package com.documentanalysis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
public class Document {
    @Id
    private String id;
    
    @Column(name = "filename")
    private String filename;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "file_hash")
    private String fileHash;
    
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_type")
    private String fileType;
}