package com.qtai.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * BFF는 외부 호출이 많아 전용 Executor를 분리한다.
 *
 * <p>core 8 / max 32. 외부 호출은 IO bound이므로 max 크게.
 * <p>TODO(강태오): JaegerTracing context propagation TaskDecorator.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "bibleExecutor")
    public Executor bibleExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(8);
        exec.setMaxPoolSize(32);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("bff-bible-");
        exec.initialize();
        return exec;
    }
}
