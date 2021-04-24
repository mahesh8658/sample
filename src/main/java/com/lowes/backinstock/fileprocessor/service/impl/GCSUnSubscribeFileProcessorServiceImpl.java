package com.lowes.backinstock.fileprocessor.service.impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.lowes.backinstock.fileprocessor.configuration.GCSBasicConfig;
import com.lowes.backinstock.fileprocessor.configuration.GCSUnSubscribeFilePathsConfig;
import com.lowes.backinstock.fileprocessor.mapper.UnSubscribeFileToKillSwitchRequest;
import com.lowes.backinstock.fileprocessor.service.GCSUnSubscribeFileProcessorService;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import static com.lowes.backinstock.fileprocessor.constants.GCSFileProcessorConstants.*;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

@Service
@Slf4j
public class GCSUnSubscribeFileProcessorServiceImpl implements GCSUnSubscribeFileProcessorService {

    @Autowired
    private GCSBasicConfig gcsBasicConfig;

    @Autowired
    private GCSUnSubscribeFilePathsConfig gcsUnSubscribeFilePathsConfig;

    @Autowired
    private Storage storage;


    @Override
    public void deleteUnSubscribeFileFromGCSBucket(String fileName) {
        try {
            final String filePathAndName = new StringBuilder(gcsUnSubscribeFilePathsConfig.getInboundPath()).append(fileName).toString();
            Blob blob = storage.get(gcsBasicConfig.getBucketName(), filePathAndName);
            blob.delete();
            log.info("UNSUBSCRIBE_FILE_DELETED_FROM {} ", blob.getMediaLink());
        } catch (Exception exception) {
            log.error("EXCEPTION_OCCURRED_WHILE_DELETING_FILE {} ", exception);
        }
    }

    @Override
    public void moveToErrorLocationAndDelete(String fileName, String errorReason) {
        try {
            final String filePathAndName = new StringBuilder(gcsUnSubscribeFilePathsConfig.getInboundPath()).append(fileName).toString();
            Blob sourceBlob = storage.get(gcsBasicConfig.getBucketName(), filePathAndName);
            final BlobId sourceBlobId = sourceBlob.getBlobId();
            final String destinationBucket = gcsBasicConfig.getBucketName();
            String destinationFileName = fileName.replace(CSV_FILE_EXT, new StringBuilder(UNDERSCORE_DELIM).append(errorReason).append(CSV_FILE_EXT).toString());
            final String destinationPathAndName = new StringBuilder(gcsUnSubscribeFilePathsConfig.getErrorPath()).append(destinationFileName).toString();
            log.debug(
                    "COPYING_OBJECT '{}' TO_OBJECT '{}'",
                    String.format(BLOB_PATH_TEMPLATE, sourceBlobId.getBucket(), sourceBlobId.getName()),
                    String.format(BLOB_PATH_TEMPLATE, destinationBucket, destinationPathAndName)
            );

            Storage.CopyRequest copyRequest = new Storage.CopyRequest.Builder()
                    .setSource(sourceBlobId)
                    .setTarget(BlobId.of(destinationBucket, destinationPathAndName))
                    .build();
            Blob destinationBlob = storage.copy(copyRequest).getResult();
            log.debug(
                    "COPIED_OBJECT '{}' TO_OBJECT '{}'",
                    String.format(BLOB_PATH_TEMPLATE, sourceBlobId.getBucket(), sourceBlobId.getName()),
                    String.format(BLOB_PATH_TEMPLATE, destinationBlob.getBlobId().getBucket(), destinationBlob.getBlobId().getName())
            );
            sourceBlob.delete();
            log.info("UNSUBSCRIBE_FILE_MOVED_AND_DELETED_FROM {} ", sourceBlob.getMediaLink());
        } catch (Exception exception) {
            log.error("EXCEPTION_OCCURRED_WHILE_MOVING_TO_ERROR_LOCATION {} ", exception);
        }
    }


    @Override
    public void addSuccessUnSubscribeCSVFileInGCSBucket(String fileName, List<UnSubscribeFileToKillSwitchRequest> successRecords) {
        log.info("PROCESSING_SUCCESS_RECORDS {}, FILE_NAME {}", successRecords, fileName);
        final String filePathAndName = new StringBuilder(gcsUnSubscribeFilePathsConfig.getProcessedPath()).append(fileName).toString();
        addUnSubscribeCSVFileInGCSBucket(filePathAndName, successRecords);
    }

    @Override
    public void add4xxErrorsUnSubscribeCSVFileInGCSBucket(String fileName, List<UnSubscribeFileToKillSwitchRequest> failure4xxRecords) {
        log.info("PROCESSING_4XX_ERROR_RECORDS {}, FILE_NAME {}", failure4xxRecords, fileName);
        fileName = fileName.replace(CSV_FILE_EXT, new StringBuilder(UNDERSCORE_DELIM).append(escapeJava("4XX")).append(CSV_FILE_EXT).toString());
        final String filePathAndName = new StringBuilder(gcsUnSubscribeFilePathsConfig.getErrorPath()).append(fileName).toString();
        addUnSubscribeCSVFileInGCSBucket(filePathAndName, failure4xxRecords);
    }

    @Override
    public void add5xxErrorsUnSubscribeCSVFileInGCSBucket(String fileName, List<UnSubscribeFileToKillSwitchRequest> failure5xxRecords) {
        log.info("PROCESSING_5XX_ERROR_RECORDS {}, FILE_NAME {}", failure5xxRecords, fileName);
        fileName = fileName.replace(CSV_FILE_EXT, new StringBuilder(UNDERSCORE_DELIM).append(escapeJava("5XX")).append(CSV_FILE_EXT).toString());
        final String filePathAndName = new StringBuilder(gcsUnSubscribeFilePathsConfig.getErrorPath()).append(fileName).toString();
        addUnSubscribeCSVFileInGCSBucket(filePathAndName, failure5xxRecords);
    }

    @Override
    public void addInvalidSubscribeFileRecordsInGCSBucket(String fileName, String errorReason, List<UnSubscribeFileToKillSwitchRequest> invalidRecords) {
        log.info("PROCESSING_INVALID_ERROR_RECORDS {}, FILE_NAME {}", invalidRecords, fileName);
        fileName = fileName.replace(CSV_FILE_EXT, new StringBuilder(UNDERSCORE_DELIM).append(errorReason).append(CSV_FILE_EXT).toString());
        final String filePathAndName = new StringBuilder(gcsUnSubscribeFilePathsConfig.getErrorPath()).append(fileName).toString();
        addUnSubscribeCSVFileInGCSBucket(filePathAndName, invalidRecords);
    }

    private void addUnSubscribeCSVFileInGCSBucket(String fileName, List<UnSubscribeFileToKillSwitchRequest> records) {
        try {
            final String gcsBucketName = gcsBasicConfig.getBucketName();
            byte[] bytes = createKillSwitchRequestsAsBeanArray(records);
            BlobId blobId = BlobId.of(gcsBucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/csv").build();
            storage.create(blobInfo, bytes);
            log.info("CREATED_THE_FILE_WITH_FILE_NAME {} ,IN_THE_BUCKET {} ", fileName, gcsBucketName);
        } catch (Exception exception) {
            log.error("EXCEPTION_OCCURRED_WHILE_CREATING_FILE_IN_GCS FILE_NAME {} EXCEPTION {} ", fileName, exception);
        }
    }

    public byte[] createKillSwitchRequestsAsBeanArray(List<UnSubscribeFileToKillSwitchRequest> records) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream)) {
                StatefulBeanToCsv<UnSubscribeFileToKillSwitchRequest> beanToCsv = new StatefulBeanToCsvBuilder<UnSubscribeFileToKillSwitchRequest>(outputStreamWriter)
                        .build();
                beanToCsv.write(records);
            }
        } catch (CsvDataTypeMismatchException csvDataTypeMismatchException) {
            log.error("CSV_DATA_MIS_MATCH_EXCEPTION_HAPPENED_ON_CREATEKILLSWITCHREQUESTSASBEANARRAY_WHILE_PROCESSING_THE_FILE_PATH {} , EXCEPTION {}",
                    csvDataTypeMismatchException);
        } catch (CsvRequiredFieldEmptyException csvRequiredFieldEmptyException) {
            log.error("CSV_REQUIRED_FILE_EMPTY_EXCEPTION_HAPPENED_ON_CREATEKILLSWITCHREQUESTSASBEANARRAY_WHILE_PROCESSING_THE_FILE_PATH {} , EXCEPTION {}",
                    csvRequiredFieldEmptyException);
        } catch (IOException iOException) {
            log.error("IOEXCEPTION_HAPPENED_ON_CREATEKILLSWITCHREQUESTSASBEANARRAY_WHILE_PROCESSING_THE_FILE_PATH {} , EXCEPTION {}", iOException);
        } catch (Exception exception) {
            log.error("EXCEPTION_HAPPENED_ON_CREATEKILLSWITCHREQUESTSASBEANARRAY_WHILE_PROCESSING_THE_FILE_PATH {} , EXCEPTION {}", exception);
        }
        return stream.toByteArray();
    }
}
