package com.zigu.ziguwas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

@Configuration
public class MultipartJacksonConfig {

    public MultipartJacksonConfig(MappingJackson2HttpMessageConverter converter) {
        // octet-stream(바이너리)도 JSON 컨버터가 처리할 수 있도록 허용
        List<MediaType> supportedTypes = new java.util.ArrayList<>(converter.getSupportedMediaTypes());
        supportedTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        converter.setSupportedMediaTypes(supportedTypes);
    }
}
