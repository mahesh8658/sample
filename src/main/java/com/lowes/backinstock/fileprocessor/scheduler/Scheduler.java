package com.lowes.backinstock.fileprocessor.scheduler;

import com.lowes.backinstock.fileprocessor.service.GCSFileReaderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Scheduler {

    @Autowired
    private GCSFileReaderService gcsFileReaderService;

    @Scheduled(cron = "${scheduler.unSubscribeFile.cron.expression}", zone = "${scheduler.unSubscribeFile.cron.zone}")
    public void unSubscribeFileScheduler() {
        log.info("RUNNING_UNSUBSCRIBE_FILE_SCHEDULER");
        try {
            gcsFileReaderService.processKillSwitchRequests();
        } catch (Exception exception) {
            log.error("EXCEPTION_OCCURRED_ON_UNSUBSCRIBE_FILE_SCHEDULER ", exception);
        }
    }
}
