package com.omis5.fileStorageService.controllers;

import com.omis5.fileStorageService.services.FileStorageService;
import lombok.Data;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
public class FilesController {
    private final FileStorageService fileStorageService;

    public FilesController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Data
    public static class Metadata {
        Long taskId;
    }
    @PostMapping("save")
    public ResponseEntity<?> save(@RequestParam("file") MultipartFile file, @RequestBody Metadata metadata ) {
        try {
            fileStorageService.save(file, metadata);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PostMapping("download")
    public ResponseEntity<File> download(@RequestParam("task_id") long taskId) {
        try {

            return new ResponseEntity<File>(fileStorageService.get(taskId),HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
