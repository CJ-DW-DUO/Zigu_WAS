package com.zigu.ziguwas.push;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class PushConfig {

    /**
     * Expo Push API 호출 전용 RestTemplate입니다.
     * 타임아웃을 짧게 잡아, 외부 API(exp.host)가 느려지거나 응답이 없어도
     * 알림 생성 흐름(스케줄러/이벤트 리스너)이 오래 멈춰있지 않도록 합니다.
     */
    @Bean
    public RestTemplate expoRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
