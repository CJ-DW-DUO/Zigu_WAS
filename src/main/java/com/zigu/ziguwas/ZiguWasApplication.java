package com.zigu.ziguwas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ZiguWasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZiguWasApplication.class, args);
    }

}
