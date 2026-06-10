package com.telemed.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    String uploadFile(String bucketName, String objectName, MultipartFile file);

    String uploadString(String content, String objectName);

    byte[] downloadFile(String bucketName, String objectName);

    String getFileUrl(String bucketName, String objectName);

    void deleteFile(String bucketName, String objectName);

    void createBucketIfNotExists(String bucketName);
}
