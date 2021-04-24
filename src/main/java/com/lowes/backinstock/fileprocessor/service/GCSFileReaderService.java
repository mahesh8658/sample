package com.lowes.backinstock.fileprocessor.service;

import org.springframework.stereotype.Service;

@Service
public interface GCSFileReaderService {

    void processKillSwitchRequests();
}
