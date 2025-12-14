package com.omis5.fileStorageService.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configurable
public class S3Config {
    @Value ("${accessKey.s3}")
    private final String accessKey;
    @Value ("${secretAccessKey.s3}")
    private final String secretAccessKey;
    @Value ("${region.s3.s3}")
    private final String region;
    public S3Config(String accessKey, String secretAccessKey, String region) {
        this.accessKey = accessKey;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
    }

    @Bean
    public S3Client s3Client(){
        AwsCredentials credentials= AwsBasicCredentials.create(accessKey, secretAccessKey);

       return S3Client.builder().region(Region.of(region)).
                credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
    }

}
