package com.company.settlement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * 배치 수동 실행 요청 DTO
 */
public record BatchTriggerRequest(
    @NotNull(message = "Target date is required")
    @PastOrPresent(message = "Target date must be in the past or present")
    LocalDate targetDate
) {
}
