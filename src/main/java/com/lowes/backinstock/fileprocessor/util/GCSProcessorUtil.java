package com.lowes.backinstock.fileprocessor.util;

import com.lowes.backinstock.fileprocessor.configuration.GCSProcessorConfig;
import com.lowes.backinstock.fileprocessor.configuration.GCSProcessorConfig.RequestHeaders;
import com.lowes.backinstock.fileprocessor.mapper.UnSubscribeFileToKillSwitchRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lowes.backinstock.fileprocessor.constants.GCSFileProcessorConstants.*;

@Slf4j
@UtilityClass
public class GCSProcessorUtil {

    public static HttpHeaders addHttpHeaders(RequestHeaders requestHeaders) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (requestHeaders != null) {
            final List<GCSProcessorConfig.Header> requestHeadersList = requestHeaders.getList();
            if (CollectionUtils.isNotEmpty(requestHeadersList)) {
                requestHeadersList.stream()
                        .forEach(requestHeader -> httpHeaders.add(requestHeader.getName(), requestHeader.getValue()));
                return httpHeaders;
            }
        }
        return httpHeaders;
    }

    public static void populateRecordsMapByFailureAndSuccess(UnSubscribeFileToKillSwitchRequest killSwitchRequest,
                                                             Map<String, List<UnSubscribeFileToKillSwitchRequest>> recordsBySuccessAndFailure,
                                                             String status) {
        List<UnSubscribeFileToKillSwitchRequest> failureEmailNotifyRecords = recordsBySuccessAndFailure.get(status);
        if (CollectionUtils.isNotEmpty(failureEmailNotifyRecords)) {
            failureEmailNotifyRecords.add(killSwitchRequest);
        } else {
            ArrayList<UnSubscribeFileToKillSwitchRequest> killSwitchRequests = new ArrayList<>();
            killSwitchRequests.add(killSwitchRequest);
            recordsBySuccessAndFailure.put(status, killSwitchRequests);
        }
    }

    public static void createInvalidAndValidRecordsMap(UnSubscribeFileToKillSwitchRequest killSwitchRequest,
                                                       HashMap<String, List<UnSubscribeFileToKillSwitchRequest>> invalidAndValidRecordsMap) {
        final String storeId = killSwitchRequest.getStoreId();
        if (StringUtils.isBlank(killSwitchRequest.getEmailId())
                && StringUtils.isBlank(killSwitchRequest.getOmniItemId())
                && StringUtils.isBlank(storeId)) {
            GCSProcessorUtil.populateRecordsMapByFailureAndSuccess(killSwitchRequest, invalidAndValidRecordsMap, INVALID_RECORDS);
        } else if (StringUtils.isBlank(killSwitchRequest.getEmailId())) {
            GCSProcessorUtil.populateRecordsMapByFailureAndSuccess(killSwitchRequest, invalidAndValidRecordsMap, EMAIL_ID_MISSING);
        } else if (StringUtils.isBlank(killSwitchRequest.getOmniItemId())) {
            GCSProcessorUtil.populateRecordsMapByFailureAndSuccess(killSwitchRequest, invalidAndValidRecordsMap, OMNI_ITEM_ID_MISSING);
        } else if (StringUtils.isBlank(storeId)) {
            GCSProcessorUtil.populateRecordsMapByFailureAndSuccess(killSwitchRequest, invalidAndValidRecordsMap, STORE_ID_MISSING);
        } else {
            GCSProcessorUtil.populateRecordsMapByFailureAndSuccess(killSwitchRequest, invalidAndValidRecordsMap, VALID_RECORDS);
        }
        if (StringUtils.isNotBlank(storeId) && StringUtils.isNumeric(storeId) && storeId.length() < 4) {
            final String correctedStoreId = String.format("%04d", Integer.valueOf(storeId));
            log.info("STORE_ID_IS_LESS_THAN_4_HENCE_ADDED_PADDING_ZERO_BEFORE_PROCESSING {}", correctedStoreId);
            killSwitchRequest.setStoreId(correctedStoreId);
        }
    }
}


