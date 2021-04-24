package com.lowes.backinstock.fileprocessor.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "configuration.gcs")
@Data
@Component
public class GCSBasicConfig {
    private String projectId;
    private String bucketName;
    private String hostName;
    private String credentialsPath;
}
