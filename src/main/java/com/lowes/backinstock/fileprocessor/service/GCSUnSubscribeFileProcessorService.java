package com.lowes.backinstock.fileprocessor.service;

import com.lowes.backinstock.fileprocessor.mapper.UnSubscribeFileToKillSwitchRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface GCSUnSubscribeFileProcessorService {

    void deleteUnSubscribeFileFromGCSBucket(String fileName);

    void moveToErrorLocationAndDelete(String fileName, String errorReason);

    void addSuccessUnSubscribeCSVFileInGCSBucket(String fileName, List<UnSubscribeFileToKillSwitchRequest> successRecords);

    void add4xxErrorsUnSubscribeCSVFileInGCSBucket(String fileName, List<UnSubscribeFileToKillSwitchRequest> failure4xxRecords);

    void add5xxErrorsUnSubscribeCSVFileInGCSBucket(String fileName, List<UnSubscribeFileToKillSwitchRequest> failure5xxRecords);

    void addInvalidSubscribeFileRecordsInGCSBucket(String fileName,String errorReason, List<UnSubscribeFileToKillSwitchRequest> invalidRecords);
}
