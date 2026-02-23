package com.app.docflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "docflow")
public class DocflowProperties {

    private int batchSize = 100;
    private Workers workers = new Workers();
    private Scheduler scheduler = new Scheduler();

    @Getter
    @Setter
    public static class Workers {

        private boolean enabled = true;
        private long submitDelayMs = 3000;
        private long approveDelayMs = 3000;

    }

    @Getter
    @Setter
    public static class Scheduler {

        private int poolSize = 2;
        private String threadNamePrefix = "docflow-worker-";
        private int awaitTerminationSeconds = 5;

    }

}
