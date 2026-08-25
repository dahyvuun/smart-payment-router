package com.dahyvuun.payment_router.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FailureAnalysisResponse {
    private String currentFailureReason;
    private List<String> similarPastFailures;
    private String analysis;
}