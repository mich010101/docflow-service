package com.app.docflow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DocflowProperties.class)
@RequiredArgsConstructor
public class AppBeansConfig {

    private final DocflowProperties docflowProperties;

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(docflowProperties.getScheduler().getPoolSize());
        scheduler.setThreadNamePrefix(docflowProperties.getScheduler().getThreadNamePrefix());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(docflowProperties.getScheduler().getAwaitTerminationSeconds());
        return scheduler;
    }
}
