package com.lowes.backinstock.fileprocessor.service.impl;

import com.lowes.backinstock.fileprocessor.configuration.GCSUnSubscribeFilePathsConfig;
import com.lowes.backinstock.fileprocessor.mapper.UnSubscribeFileToKillSwitchRequest;
import com.lowes.backinstock.fileprocessor.service.GCSFileReaderService;
import com.lowes.backinstock.fileprocessor.service.GCSUnSubscribeFileProcessorService;
import com.lowes.backinstock.fileprocessor.service.KillSwitchClientService;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.lowes.backinstock.fileprocessor.constants.GCSFileProcessorConstants.*;
import static com.lowes.backinstock.fileprocessor.util.GCSProcessorUtil.createInvalidAndValidRecordsMap;
import static java.nio.file.Files.newDirectoryStream;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Service
@Slf4j
public class GCSFileReaderServiceImpl implements GCSFileReaderService {

    @Autowired
    private FileSystem fileSystem;

    @Autowired
    private GCSUnSubscribeFilePathsConfig gcsUnSubscribeFilePathsConfig;

    @Autowired
    private KillSwitchClientService killSwitchClientService;

    @Autowired
    private GCSUnSubscribeFileProcessorService gcsUnSubscribeFileProcessorService;


    @Override
    public void processKillSwitchRequests() {
        final Map<String, Set<UnSubscribeFileToKillSwitchRequest>> fileNameAndKillSwitchRequestsMap = getFileNameAndKillSwitchRequestsMap();
        if (MapUtils.isNotEmpty(fileNameAndKillSwitchRequestsMap)) {
            log.info("FILENAME_AND_KILL_SWITCH_REQUESTS_MAP_TO_PROCESS {}", fileNameAndKillSwitchRequestsMap);
            fileNameAndKillSwitchRequestsMap
                    .entrySet()
                    .parallelStream()
                    .forEach(fileNameAndKillSwitchRequestsEntrySet -> killSwitchClientService
                            .processKillSwitchRequests(fileNameAndKillSwitchRequestsEntrySet.getValue(),
                                    fileNameAndKillSwitchRequestsEntrySet.getKey()));
        } else {
            log.info("NO_FILES_TO_PROCESS_IN_BIS_GCS_BUCKETS");
        }
    }

    public Map<String, Set<UnSubscribeFileToKillSwitchRequest>> getFileNameAndKillSwitchRequestsMap() {
        HashMap<String, Set<UnSubscribeFileToKillSwitchRequest>> fileNameAndKillSwitchRequestsMap = new HashMap<>();
        final Path fileSystemPath = fileSystem.getPath(gcsUnSubscribeFilePathsConfig.getInboundPath());
        DirectoryStream.Filter<Path> filter = file -> file.getFileName().toString().endsWith(CSV_FILE_EXT)
                && file.getFileName().toString().startsWith(LOWES_PREFIX);
        HeaderColumnNameMappingStrategy<UnSubscribeFileToKillSwitchRequest> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(UnSubscribeFileToKillSwitchRequest.class);
        try (DirectoryStream<Path> directoryStream = newDirectoryStream(fileSystemPath, filter)) {
            for (Path path : directoryStream) {
                final String fileName = path.getFileName().toString();
                if (!Files.isDirectory(path)) {
                    String filePath = path.toString();
                    List<UnSubscribeFileToKillSwitchRequest> unSubscribeFileToKillSwitchRequestMappings = getUnSubscribeFileToKillSwitchRequestMappings(strategy, path);
                    if (isNotEmpty(unSubscribeFileToKillSwitchRequestMappings)) {
                        final HashMap<String, List<UnSubscribeFileToKillSwitchRequest>> inValidAndValidRecordsMap = new HashMap<>();
                        unSubscribeFileToKillSwitchRequestMappings
                                .stream()
                                .forEach(killSwitchRequest -> createInvalidAndValidRecordsMap(killSwitchRequest, inValidAndValidRecordsMap));
                        if (MapUtils.isNotEmpty(inValidAndValidRecordsMap)) {
                            log.debug("INVALID_AND_VALID_RECORDS_MAP {}", inValidAndValidRecordsMap);
                            processInvalidAndValidRecords(fileNameAndKillSwitchRequestsMap, fileName, filePath, inValidAndValidRecordsMap);
                        } else {
                            log.error("INVALID_AND_VALID_RECORDS_MAP_EMPTY_HENCE_MOVED_THE_FILE PATH {} AND FILE_NAME {}", filePath, fileName);
                            gcsUnSubscribeFileProcessorService.moveToErrorLocationAndDelete(fileName, "INVALID_RECORDS_FILE");
                        }
                    } else {
                        log.error("NO_RECORDS_TO_PROCESS_HENCE_MOVED_THE_FILE PATH {} AND FILE_NAME {}", filePath, fileName);
                        gcsUnSubscribeFileProcessorService.moveToErrorLocationAndDelete(fileName, "INVALID_FILE");
                    }
                }
            }
        } catch (IOException ioException) {
            log.error("IOEXCEPTION_OCCURRED_ON_GETFILENAMEANDKILLSWITCHREQUESTSMAP_WHILE_PROCESSING_FILE_DIRECTORY {}", ioException);
        } catch (Exception exception) {
            log.error("EXCEPTION_OCCURRED_ON_GETFILENAMEANDKILLSWITCHREQUESTSMAP_WHILE_PROCESSING_FILE_DIRECTORY {} ", exception);
        }
        return fileNameAndKillSwitchRequestsMap;
    }

    private void processInvalidAndValidRecords(HashMap<String, Set<UnSubscribeFileToKillSwitchRequest>> fileNameAndKillSwitchRequestsMap,
                                               String fileName, String filePath,
                                               HashMap<String, List<UnSubscribeFileToKillSwitchRequest>> invalidAndValidRecordsMap) {
        final List<UnSubscribeFileToKillSwitchRequest> validKillSwitchRequests = invalidAndValidRecordsMap.get(VALID_RECORDS);
        final List<UnSubscribeFileToKillSwitchRequest> inValidKillSwitchRequests = invalidAndValidRecordsMap.get(INVALID_RECORDS);
        final List<UnSubscribeFileToKillSwitchRequest> omniIdMissingKillSwitchRequests = invalidAndValidRecordsMap.get(OMNI_ITEM_ID_MISSING);
        final List<UnSubscribeFileToKillSwitchRequest> emailIdMissingKillSwitchRequests = invalidAndValidRecordsMap.get(EMAIL_ID_MISSING);
        final List<UnSubscribeFileToKillSwitchRequest> storeIdMissingKillSwitchRequests = invalidAndValidRecordsMap.get(STORE_ID_MISSING);
        final boolean validKillSwitchRequestsListNotEmpty = isNotEmpty(validKillSwitchRequests);
        if (validKillSwitchRequestsListNotEmpty) {
            log.info("AVAILABLE_KILL_SWITCH_REQUESTS {} FILE_ABS_PATH {}", validKillSwitchRequests, filePath);
            Set<UnSubscribeFileToKillSwitchRequest> unSubscribeFileToKillSwitchRequestMapping = new HashSet<>(validKillSwitchRequests);
            unSubscribeFileToKillSwitchRequestMapping.addAll(validKillSwitchRequests);
            fileNameAndKillSwitchRequestsMap.put(fileName, unSubscribeFileToKillSwitchRequestMapping);
        } else {
            log.warn("NO_VALID_KILL_SWITCH_REQUESTS_TO_PROCESS_FOR_FILE_ABS_PATH {} FILE_NAME {}", filePath, fileName);
        }
        if (isNotEmpty(omniIdMissingKillSwitchRequests)) {
            log.error("OMNI_ITEM_IDS_MISSING_KILL_SWITCH_REQUESTS {} FILE_ABS_PATH {} FILE_NAME {}", omniIdMissingKillSwitchRequests, filePath, fileName);
            gcsUnSubscribeFileProcessorService.addInvalidSubscribeFileRecordsInGCSBucket(fileName, OMNI_ITEM_ID_MISSING, omniIdMissingKillSwitchRequests);
            if (!validKillSwitchRequestsListNotEmpty) {
                gcsUnSubscribeFileProcessorService.deleteUnSubscribeFileFromGCSBucket(fileName);
            }
        }
        if (isNotEmpty(emailIdMissingKillSwitchRequests)) {
            log.error("EMAIL_IDS_MISSING_KILL_SWITCH_REQUESTS {} FILE_ABS_PATH {} FILE_NAME {}", emailIdMissingKillSwitchRequests, filePath, fileName);
            gcsUnSubscribeFileProcessorService.addInvalidSubscribeFileRecordsInGCSBucket(fileName, EMAIL_ID_MISSING, emailIdMissingKillSwitchRequests);
            if (!validKillSwitchRequestsListNotEmpty) {
                gcsUnSubscribeFileProcessorService.deleteUnSubscribeFileFromGCSBucket(fileName);
            }
        }
        if (isNotEmpty(storeIdMissingKillSwitchRequests)) {
            log.error("STORE_IDS_MISSING_KILL_SWITCH_REQUESTS {} FILE_ABS_PATH {} FILE_NAME {}", storeIdMissingKillSwitchRequests, filePath, fileName);
            gcsUnSubscribeFileProcessorService.addInvalidSubscribeFileRecordsInGCSBucket(fileName, STORE_ID_MISSING, storeIdMissingKillSwitchRequests);
            if (!validKillSwitchRequestsListNotEmpty) {
                gcsUnSubscribeFileProcessorService.deleteUnSubscribeFileFromGCSBucket(fileName);
            }
        }
        if (isNotEmpty(inValidKillSwitchRequests)) {
            log.error("INVALID_KILL_SWITCH_REQUESTS {} FILE_ABS_PATH {} FILE_NAME {}", inValidKillSwitchRequests, filePath, fileName);
            gcsUnSubscribeFileProcessorService.addInvalidSubscribeFileRecordsInGCSBucket(fileName, INVALID_RECORDS, inValidKillSwitchRequests);
            if (!validKillSwitchRequestsListNotEmpty) {
                gcsUnSubscribeFileProcessorService.deleteUnSubscribeFileFromGCSBucket(fileName);
            }
        }
    }

    private List<UnSubscribeFileToKillSwitchRequest> getUnSubscribeFileToKillSwitchRequestMappings(HeaderColumnNameMappingStrategy<UnSubscribeFileToKillSwitchRequest> strategy,
                                                                                                   Path filePath) {
        List<UnSubscribeFileToKillSwitchRequest> killSwitchRequests = new ArrayList<>();

        try {
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(new BOMInputStream(Files.newInputStream(filePath)), StandardCharsets.UTF_8))) {
                final CsvToBean<UnSubscribeFileToKillSwitchRequest> unSubscribeFileBeanMappings = new CsvToBeanBuilder<UnSubscribeFileToKillSwitchRequest>(csvReader)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withSkipLines(0)
                        .withFieldAsNull(CSVReaderNullFieldIndicator.NEITHER)
                        .withIgnoreEmptyLine(true)
                        .withMappingStrategy(strategy)
                        .build();
                killSwitchRequests = unSubscribeFileBeanMappings.parse();
            }
        } catch (IOException ioException) {
            log.error("IOEXCEPTION_OCCURRED_ON_GETUNSUBSCRIBEFILETOKILLSWITCHREQUESTMAPPINGS_WHILE_PROCESSING_FILE {} FILE_ABS_PATH {} ",
                    ioException, filePath);
        } catch (Exception exception) {
            log.error("EXCEPTION_OCCURRED_ON_GETUNSUBSCRIBEFILETOKILLSWITCHREQUESTMAPPINGS_WHILE_PROCESSING_FILE {} FILE_ABS_PATH {} ",
                    exception, filePath);
        }
        return killSwitchRequests;
    }
}
