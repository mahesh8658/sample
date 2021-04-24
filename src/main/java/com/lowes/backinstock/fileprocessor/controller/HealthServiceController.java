package com.lowes.backinstock.fileprocessor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class HealthServiceController {
    @GetMapping("/health")
    public ResponseEntity<String> getHealthStatus() {
        return ResponseEntity.status(HttpStatus.OK).body("SUCCESS");
    }
}
