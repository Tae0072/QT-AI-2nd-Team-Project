package com.qtai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QtAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QtAiApplication.class, args);
    }
}
