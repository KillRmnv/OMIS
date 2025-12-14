package com.omis5.fileStorageService.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="source_code_files")
@Data
public class SourceCodeFiles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false)
    private long taskId;
}
