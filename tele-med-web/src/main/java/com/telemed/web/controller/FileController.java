package com.telemed.web.controller;

import com.telemed.common.result.Result;
import com.telemed.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private MinioService minioService;

    @Value("${minio.bucketName}")
    private String bucketName;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam MultipartFile file,
                                  @RequestParam(required = false) String objectName) {
        String url = minioService.uploadFile(bucketName, objectName, file);
        return Result.ok(url);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String objectName) {
        byte[] bytes = minioService.downloadFile(bucketName, objectName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/url")
    public Result<String> getUrl(@RequestParam String objectName) {
        String url = minioService.getFileUrl(bucketName, objectName);
        return Result.ok(url);
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam String objectName) {
        minioService.deleteFile(bucketName, objectName);
        return Result.ok();
    }
}
