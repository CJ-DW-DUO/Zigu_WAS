package com.zigu.ziguwas.S3;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    @Bean
    public AmazonS3 amazonS3Client() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        // S3 응답 지연 시 요청 스레드가 무한정 대기하지 않도록 타임아웃을 명시적으로 설정
        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withConnectionTimeout(5_000)
                .withSocketTimeout(10_000);

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials));

        if (endpoint != null && !endpoint.isEmpty()) {
            // 로컬스택용 가짜 S3 설정
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                    .withPathStyleAccessEnabled(true);
        } else {
            // 실제 AWS S3 설정
            builder.withRegion(region);
        }

        return builder.build();
    }
}
