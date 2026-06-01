package com.zzy.aurenteasebackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class AwsResourceInitializer implements CommandLineRunner {
    //大厂标准，严谨使用System.out.println, 必须使用标准的SLF4J日志框架
    private static final Logger log = LoggerFactory.getLogger(AwsResourceInitializer.class);

    private final S3Client s3Client;

    private final String bucketName;

    public AwsResourceInitializer(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }


    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing Rentease Cloud Infrastructure: Checking AWS S3 Bucket...");

        try {
            //探测云端，检查指定的存储桶是否存在
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("AWS S3 Bucket '{}' already exists. Cloud infrastructure is ready.", bucketName);

        } catch (S3Exception e) {
            //AWS SDK V2如何探测到桶不存在（404），会直接抛出S3Exception
            if (e.statusCode() == 404) {
                log.warn("AWS S3 Bucket '{}' does not found. Activating Self-Healing infrastructure", bucketName);

                //自动自愈：调用AWS官方的API动态创建桶
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("Successfully self-healed! AWS S3 Bucket '{}' created in cloud environment.", bucketName);
            } else {
                //如果是其他的云端错误（如凭证完全错误，网络彻底断开），记录Error日志
                log.error("Fail to connect to AWS S3 Bucket due to unexpected error");
                throw e;//抛出异常阻止程序启动，防止带病上线
            }
        }
    }
}
