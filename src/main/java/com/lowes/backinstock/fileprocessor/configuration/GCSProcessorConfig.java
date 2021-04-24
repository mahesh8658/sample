package com.lowes.backinstock.fileprocessor.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "configuration")
@Data
@Component
public class GCSProcessorConfig {

    private KillSwitchClient killSwitchClient = new KillSwitchClient();

    @Data
    public static class KillSwitchClient {
        private long backOffPeriod;
        private int maxAttempts;
        private RequestHeaders requestHeaders = new RequestHeaders();
    }

    @Data
    public static class RequestHeaders {
        private List<Header> list = new ArrayList<>();
    }

    @Data
    public static class Header {
        private String name;
        private String value;
    }
}
