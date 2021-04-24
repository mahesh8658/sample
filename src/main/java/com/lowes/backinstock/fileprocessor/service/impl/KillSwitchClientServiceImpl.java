package com.lowes.backinstock.fileprocessor.service.impl;

import com.lowes.backinstock.fileprocessor.client.killswitch.KillSwitchClient;
import com.lowes.backinstock.fileprocessor.configuration.GCSProcessorConfig;
import com.lowes.backinstock.fileprocessor.mapper.UnSubscribeFileToKillSwitchRequest;
import com.lowes.backinstock.fileprocessor.service.GCSUnSubscribeFileProcessorService;
import com.lowes.backinstock.fileprocessor.service.KillSwitchClientService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.lowes.backinstock.fileprocessor.constants.GCSFileProcessorConstants.*;
import static com.lowes.backinstock.fileprocessor.util.GCSProcessorUtil.addHttpHeaders;
import static com.lowes.backinstock.fileprocessor.util.GCSProcessorUtil.populateRecordsMapByFailureAndSuccess;

@Service
@Slf4j
public class KillSwitchClientServiceImpl implements KillSwitchClientService {

    @Autowired
    private KillSwitchClient killSwitchClient;

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private GCSProcessorConfig gcsProcessorConfig;

    @Autowired
    private GCSUnSubscribeFileProcessorService gcsUnSubscribeFileProcessorService;

    @PostConstruct
    private void addPoliciesForRetryTemplate() {
        final GCSProcessorConfig.KillSwitchClient killSwitchClientConfig = gcsProcessorConfig.getKillSwitchClient();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(killSwitchClientConfig.getBackOffPeriod());
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(killSwitchClientConfig.getMaxAttempts());
    }

    @Override
    public Map<String, Set<UnSubscribeFileToKillSwitchRequest>> multiCallKillSwitchAsync(Set<UnSubscribeFileToKillSwitchRequest> killSwitchRequests) {
        return null;
    }

    @Override
    public ResponseEntity<Object> callKillSwitchAPI(UnSubscribeFileToKillSwitchRequest killSwitchRequest) {
        final GCSProcessorConfig.RequestHeaders requestHeaders = gcsProcessorConfig.getKillSwitchClient().getRequestHeaders();
        return retryTemplate.execute(retryCallBack -> {
            ResponseEntity<Object> killSwitchResponseEntity = null;
            try {
                killSwitchResponseEntity = killSwitchClient.killSwitch(killSwitchRequest, addHttpHeaders(requestHeaders));
            } catch (FeignException feignException) {
                log.error("ERROR_OCCURRED_WHILE_CALLING_THE_BIS_KILL_SWITCH_API {} AND_STATUS_CODE {}", feignException, feignException.status());
                if (HttpStatus.valueOf(feignException.status()).is4xxClientError()) {
                    retryCallBack.setExhaustedOnly();
                    killSwitchResponseEntity = new ResponseEntity<Object>(feignException.getMessage(), HttpStatus.valueOf(feignException.status()));
                } else {
                    throw new RetryException("RETRY_EXCEPTION_OCCURRED_ON_KILL_SWITCH_CLIENT_SERVICE {}", feignException);
                }
            }
            return killSwitchResponseEntity;
        });
    }

    @Override
    public HashMap<String, List<UnSubscribeFileToKillSwitchRequest>> processSingleKillSwitchRequest(UnSubscribeFileToKillSwitchRequest killSwitchRequest,
                                                                                                    HashMap<String, List<UnSubscribeFileToKillSwitchRequest>> httpStatusAndKillSwitchRequests) {
        final ResponseEntity<Object> killSwitchResponseEntity = callKillSwitchAPI(killSwitchRequest);
        if (killSwitchResponseEntity != null
                && killSwitchResponseEntity.getStatusCode().is2xxSuccessful()) {
            populateRecordsMapByFailureAndSuccess(killSwitchRequest,
                    httpStatusAndKillSwitchRequests,
                    SUCCESS_2XX);
        } else if (killSwitchResponseEntity != null
                && killSwitchResponseEntity.getStatusCode().is4xxClientError()) {
            final Object killSwitchResponseEntityBody = killSwitchResponseEntity.getBody();
            if (killSwitchResponseEntityBody instanceof String) {
                killSwitchRequest.setErrorMessage(StringEscapeUtils.unescapeJava((String) killSwitchResponseEntityBody));
            }
            populateRecordsMapByFailureAndSuccess(killSwitchRequest,
                    httpStatusAndKillSwitchRequests,
                    FAILURES_4XX);
        } else {
            populateRecordsMapByFailureAndSuccess(killSwitchRequest,
                    httpStatusAndKillSwitchRequests,
                    FAILURES_5XX);
        }
        return httpStatusAndKillSwitchRequests;
    }

    @Override
    public void processKillSwitchRequests(Set<UnSubscribeFileToKillSwitchRequest> killSwitchRequests,
                                          String fileName) {
        HashMap<String, List<UnSubscribeFileToKillSwitchRequest>> httpStatusAndKillSwitchRequests = new HashMap<>();
        killSwitchRequests
                .parallelStream()
                .forEach(killSwitchRequest ->
                        processSingleKillSwitchRequest(killSwitchRequest,
                                httpStatusAndKillSwitchRequests));
        log.info("HTTP_STATUS_AND_KILL_SWITCH_REQ_MAP {} ", httpStatusAndKillSwitchRequests);
        if (MapUtils.isNotEmpty(httpStatusAndKillSwitchRequests)) {
            final List<UnSubscribeFileToKillSwitchRequest> successRecords = httpStatusAndKillSwitchRequests.get(SUCCESS_2XX);
            final List<UnSubscribeFileToKillSwitchRequest> failure4xxRecords = httpStatusAndKillSwitchRequests.get(FAILURES_4XX);
            final List<UnSubscribeFileToKillSwitchRequest> failure5xxRecords = httpStatusAndKillSwitchRequests.get(FAILURES_5XX);
            if (CollectionUtils.isNotEmpty(successRecords)) {
                gcsUnSubscribeFileProcessorService.addSuccessUnSubscribeCSVFileInGCSBucket(fileName, successRecords);
            }
            if (CollectionUtils.isNotEmpty(failure4xxRecords)) {
                gcsUnSubscribeFileProcessorService.add4xxErrorsUnSubscribeCSVFileInGCSBucket(fileName, failure4xxRecords);
            }
            if (CollectionUtils.isNotEmpty(failure5xxRecords)) {
                gcsUnSubscribeFileProcessorService.add5xxErrorsUnSubscribeCSVFileInGCSBucket(fileName, failure5xxRecords);
            }
            gcsUnSubscribeFileProcessorService.deleteUnSubscribeFileFromGCSBucket(fileName);
        }
    }
}
