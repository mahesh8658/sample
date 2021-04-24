package com.lowes.backinstock.fileprocessor.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.contrib.nio.CloudStorageConfiguration;
import com.google.cloud.storage.contrib.nio.CloudStorageFileSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;

@Component
@Slf4j
public class GCSConnectionConfig {

    private GoogleCredentials credentials;

    @Autowired
    private GCSBasicConfig gcsConfig;

    @Value("${credentialsPath}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        log.info("ATTEMPTING_TO_LOAD_CREDENTIALS_FROM_CREDENTIALS_PATH: {} BUCKET: {} ",
                credentialsPath, gcsConfig.getBucketName());
        try (InputStream serviceAccountStream = new FileSystemResource(credentialsPath).getInputStream()) {
            credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
        } catch (IOException ioException) {
            log.error("IO_EXCEPTION_OCCURRED_WHILE_LOADING_THE_GOOGLE_CREDENTIALS {}", ioException);
        } catch (Exception exception) {
            log.error("EXCEPTION_OCCURRED_WHILE_LOADING_THE_GOOGLE_CREDENTIALS {}", exception);
        }
    }

    @Bean
    public StorageOptions storageOptions() {
        return StorageOptions.newBuilder().setProjectId(gcsConfig.getProjectId()).setCredentials(credentials).build();
    }

    @Bean
    public Storage storage(StorageOptions storageOptions) {
        return storageOptions.getService();
    }

    @Bean
    public FileSystem fileSystem(StorageOptions storageOptions) {
        return CloudStorageFileSystem.forBucket(gcsConfig.getBucketName(), CloudStorageConfiguration.DEFAULT, storageOptions);
    }
}
