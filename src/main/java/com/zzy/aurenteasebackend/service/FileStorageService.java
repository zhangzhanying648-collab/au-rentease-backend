package com.zzy.aurenteasebackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.print.attribute.standard.PresentationDirection;
import java.time.Duration;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final S3Presigner s3Presigner;
    private final String bucketName;

    public FileStorageService(S3Presigner s3Presigner, @Value("${aws.s3.bucket-name}")String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    /**
     * 核心能力：生成前端用来上传图片的URL（有效期15分钟）
     * @param originalFilename originalFileName 原始文件名，如 "my_kitchen.jpg"
     * @return 返回独一无二的 S3 对象 Key 和加密上传 URL
     */
    public PresignedUrlResponse generateUploadUrl(String originalFilename){
        //避坑：千万不能使用图片的原始文件名存入S3，如果两个房东是都上传了image.png,会惨遭覆盖
        //必须用UUID重命名，并保留原来的扩展名
        String fileExtension=originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueObjectKey= "properties/photos/"+UUID.randomUUID()+fileExtension;

        log.info("Generating secure S3 Presigned URL for upload. Target Key:{}", uniqueObjectKey);

        //1. 构建一个标准的AWS PUT请求图纸
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueObjectKey)
                .contentType("image/jpeg")//工业规范：严格限制上传图片的类型
                .build();

        //2. 将图纸送入签名官，并限定有效期是15分钟
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest).build();

        //3. 计算最终的数字签名链接
        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl=presignedPutObjectRequest.url().toString();

        return new PresignedUrlResponse(uniqueObjectKey,uploadUrl);
    }

    public String generateViewUrl(String objectKey){
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest=s3Presigner.presignGetObject(getObjectPresignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    //内部高标准DTO
    public record PresignedUrlResponse(String objectKey, String uploadUrl){}
}
