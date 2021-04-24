package com.lowes.backinstock.fileprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@EnableScheduling
@SpringBootApplication
public class GCPUnSubscribeFileProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(GCPUnSubscribeFileProcessorApplication.class, args);
	}
}
