package com.omis5.fileStorageService.repositories;

import com.omis5.fileStorageService.model.SourceCodeFiles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceCodeFilesRepository extends JpaRepository<SourceCodeFiles, Long> {
    SourceCodeFiles findByTaskId(long taskId);
}
