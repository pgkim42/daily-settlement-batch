package com.company.settlement.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 배치 수동 실행 응답 DTO
 */
public record BatchTriggerResponse(
    Long jobExecutionId,
    String jobName,
    LocalDate targetDate,
    String status,
    LocalDateTime startTime,
    String message
) {
    /**
     * 성공 응답 생성
     */
    public static BatchTriggerResponse success(Long jobExecutionId, String jobName,
                                                LocalDate targetDate, LocalDateTime startTime) {
        return new BatchTriggerResponse(
            jobExecutionId,
            jobName,
            targetDate,
            "STARTED",
            startTime,
            "Batch job started successfully"
        );
    }

    /**
     * 실패 응답 생성
     */
    public static BatchTriggerResponse failure(String jobName, LocalDate targetDate, String errorMessage) {
        return new BatchTriggerResponse(
            null,
            jobName,
            targetDate,
            "FAILED",
            LocalDateTime.now(),
            errorMessage
        );
    }
}
