package com.lowes.backinstock.fileprocessor.service;

import com.lowes.backinstock.fileprocessor.mapper.UnSubscribeFileToKillSwitchRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public interface KillSwitchClientService {
    Map<String, Set<UnSubscribeFileToKillSwitchRequest>> multiCallKillSwitchAsync(Set<UnSubscribeFileToKillSwitchRequest> killSwitchRequests);

    ResponseEntity<Object> callKillSwitchAPI(UnSubscribeFileToKillSwitchRequest killSwitchRequest);

    HashMap<String, List<UnSubscribeFileToKillSwitchRequest>> processSingleKillSwitchRequest(UnSubscribeFileToKillSwitchRequest killSwitchRequest,
                                  HashMap<String, List<UnSubscribeFileToKillSwitchRequest>> httpStatusAndUnSubscribeFileToKillSwitchRequest);

   void processKillSwitchRequests(Set<UnSubscribeFileToKillSwitchRequest> killSwitchRequests, String fileName);
}
