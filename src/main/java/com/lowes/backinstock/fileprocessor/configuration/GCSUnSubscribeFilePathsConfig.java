package com.lowes.backinstock.fileprocessor.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "configuration.unsubscribe-file")
@Data
@Component
public class GCSUnSubscribeFilePathsConfig {
    private String namePrefix;
    private String dateFormat;
    private String inboundPath;
    private String processedPath;
    private String errorPath;
}
