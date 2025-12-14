package com.omis5.fileStorageService.services;

import com.omis5.fileStorageService.controllers.FilesController;
import com.omis5.fileStorageService.model.SourceCodeFiles;
import com.omis5.fileStorageService.repositories.SourceCodeFilesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
@Service
public class FileStorageService {
    @Value("${bucket.name.s3.s3}")
    private final String bucketName;
    private S3Client s3Client;
    private SourceCodeFilesRepository sourceCodeFilesRepository;

    public FileStorageService( String bucketName, S3Client s3Client) {

        this.bucketName = bucketName;
        this.s3Client = s3Client;
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    public void save(MultipartFile file, FilesController.Metadata metadata) throws IOException {
        try {
            s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(file.getOriginalFilename()).
                    build(), RequestBody.fromFile(file.getResource().getFile()));
            SourceCodeFiles fileToSave=new SourceCodeFiles();
            fileToSave.setFileName(file.getOriginalFilename());
            fileToSave.setTaskId(metadata.getTaskId());
            sourceCodeFilesRepository.save(fileToSave);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public File get(long taskId) throws IOException {
        try {
            String fileName=sourceCodeFilesRepository.findByTaskId(taskId).getFileName();
            s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(fileName).
                    build(), Path.of(System.getProperty("user.dir") + File.separator+"temp_dir"+File.separator + fileName));
            return new File(System.getProperty("user.dir") + File.separator+"temp_dir"+File.separator + fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
