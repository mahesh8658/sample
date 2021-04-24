package com.lowes.backinstock.fileprocessor.client.killswitch;

import feign.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

public class KillSwitchClientConfiguration {
    @Bean
    @ConditionalOnProperty(matchIfMissing = false, prefix = "feign.client", name = "killswitch.log.level", havingValue = "full")
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
