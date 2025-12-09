package com.company.settlement.domain.enums;

/**
 * 정산 잡 실행 상태
 */
public enum SettlementJobStatus {
    /**
     * 시작됨
     */
    STARTED,

    /**
     * 완료됨
     */
    COMPLETED,

    /**
     * 실패
     */
    FAILED,

    /**
     * 부분 실패 (일부 성공)
     */
    PARTIALLY_FAILED
}