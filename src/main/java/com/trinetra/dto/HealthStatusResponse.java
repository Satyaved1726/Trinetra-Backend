package com.trinetra.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HealthStatusResponse {

    private String service;
    private String status;
    private String database;
    private String databaseName;
}