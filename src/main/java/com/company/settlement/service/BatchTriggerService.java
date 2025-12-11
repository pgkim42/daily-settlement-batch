package com.company.settlement.service;

import com.company.settlement.dto.response.BatchTriggerResponse;

import java.time.LocalDate;

/**
 * 배치 수동 실행 서비스
 */
public interface BatchTriggerService {

    /**
     * 특정 날짜의 정산 배치 수동 실행
     *
     * @param targetDate 정산 대상 날짜
     * @return 실행 결과
     */
    BatchTriggerResponse triggerSettlementBatch(LocalDate targetDate);
}
