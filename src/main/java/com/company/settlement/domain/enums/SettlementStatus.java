package com.company.settlement.domain.enums;

/**
 * 정산 상태
 */
public enum SettlementStatus {
    /**
     * 정산 처리 중
     */
    PENDING,

    /**
     * 정산 확정
     */
    CONFIRMED,

    /**
     * 지급 완료
     */
    PAID,

    /**
     * 정산 취소 (재정산 시)
     */
    CANCELLED
}