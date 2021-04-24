package com.lowes.backinstock.fileprocessor.client.killswitch;


import com.lowes.backinstock.fileprocessor.mapper.UnSubscribeFileToKillSwitchRequest;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "${feign.client.killswitch.service-name}", url = "${feign.client.killswitch.baseurl}",
        configuration = KillSwitchClientConfiguration.class)
public interface KillSwitchClient {

    @PutMapping(value = "${feign.client.killswitch.path}", consumes = "application/json")
    ResponseEntity<Object> killSwitch(@RequestBody UnSubscribeFileToKillSwitchRequest killSwitchRequest,
                                      @RequestHeader HttpHeaders httpHeaders);
}

