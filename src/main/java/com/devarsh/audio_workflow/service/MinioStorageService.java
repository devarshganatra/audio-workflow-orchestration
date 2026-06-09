package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.config.MinioConfig;
import com.devarsh.audio_workflow.config.MinioProperties;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioStorageService {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public String uploadFile(MultipartFile file,String objectKey){
        try{
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(minioProperties.bucket())
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectKey;
        }
        catch (Exception e){
            throw new RuntimeException(
                    "Failed to upload file",
                    e
            );
        }
    }

    @PostConstruct
    public void initializeBucket(){
        try{
            boolean exists=minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.bucket()).build());
            if(!exists){
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.bucket()).build());
            }

        }
        catch (Exception e){
            throw new RuntimeException(
                    "Failed to initialize MinIO bucket",
                    e
            );
        }
    }
    public InputStream downloadFile(String objectKey){
        try{
            return minioClient.getObject(GetObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                    .build());
        }
        catch (Exception e){
            throw new RuntimeException(
                    "Failed to download file",
                    e
            );
        }
    }
    public boolean objectExists(String objectKey) {
        try {

            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .build()
            );

            return true;

        } catch (Exception e) {
            return false;
        }
    }
    public StatObjectResponse getMetadata(
            String objectKey
    ) {

        try {

            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .build()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read object metadata",
                    e
            );
        }
    }
    public String uploadBytes(
            byte[] data,
            String objectKey,
            String contentType
    ) {

        try {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .stream(
                                    new ByteArrayInputStream(data),
                                    data.length,
                                    -1
                            )
                            .contentType(contentType)
                            .build()
            );

            return objectKey;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to upload object",
                    e
            );
        }
    }
}
