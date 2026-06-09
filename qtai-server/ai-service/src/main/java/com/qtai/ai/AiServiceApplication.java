package com.qtai.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        AiServiceClientConfiguration.class,
        AiServiceInboundConfiguration.class,
        AiServicePersistenceConfiguration.class,
        AiServiceUseCaseConfiguration.class,
        AiServiceWorkerConfiguration.class
})
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
