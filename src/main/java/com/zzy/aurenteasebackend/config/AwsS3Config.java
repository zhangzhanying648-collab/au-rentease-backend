package com.zzy.aurenteasebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class AwsS3Config {
    //application.yml 里找 aws.s3.endpoint。
    //如果找到了（比如本地开发填了 http://localhost:4566），就注入进来；如果压根没配置这个 key，就默认给它赋值为 null。
    @Value("${aws.s3.endpoint:#{null}}")
    private String endpoint;

    //指定 AWS 的服务机房区域。冒号后面的 ap-southeast-2 是悉尼机房（Sydney）的官方代号
    @Value("${aws.region:ap-southeast-2}")
    private String region;

    //澳洲大厂生产规范：AWS客户端必须交由Spring容器统一作为单例Bean管理
    @Bean
    public S3Client s3Client() {
        //1. 构建基础凭证，在真实AWS环境中，大厂使用IAM Role，本地沙盒可以用任意假字符串叩门
        //访问 AWS S3 就像进写字楼刷门禁卡一样，需要 Access Key（账号）和 Secret Key（密码）。
        //真实生产环境中，大厂的 EC2 服务器或 EKS 集群会绑定 IAM Role（身份角色），代码里不需要写任何明文
        // 密钥（直接用 DefaultCredentialsProvider），靠服务器自身的“刷脸”机制直接获取权限，绝对安全。
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("fake-access-key", "fake-secret-key")
        );

        var builder= S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                //极其重要： 开启PathStyle访问模式，这是兼容localstack的必要的特殊设置
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true).build());

        //2.核心避坑点：如果是本地开发环境，必须把流量重定向到我们的localstack(4566端口)
        //如果在生产环境没有配置这个endpoint，它会自动去连接真实的AWS悉尼机房，实现无缝切换
        /*
        本地开发（Local）：你在 application-dev.yml 里配置了 aws.s3.endpoint: http://localhost:4566。代码走到这里，
        发现不为主空，于是强行把流量劫持重定向到你本地的 LocalStack 容器。
        线上生产（Prod）：在线上部署时，由于真实的 AWS S3 本身就知道自己的域名，你不需要在线上配置 aws.s3.endpoint。
        这样 endpoint 变量就是 null，这个 if 分支直接被跳过，SDK 就会自动去连接 AWS 官方位于悉尼的真实生产服务器
        */
        if(endpoint != null&&!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    //注入专门用来计算预签名URL的超级签名官
    @Bean
    public S3Presigner s3Presigner() {
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("fake-access-key", "fake-secret-key")
        );
        var builder= S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true).build());;

        //兼容本地的localstack环境
        if(endpoint != null&&!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

}
