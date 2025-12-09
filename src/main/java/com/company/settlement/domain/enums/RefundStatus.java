package com.company.settlement.domain.enums;

/**
 * 환불 상태
 */
public enum RefundStatus {
    /**
     * 환불 대기
     */
    PENDING,

    /**
     * 환불 승인
     */
    APPROVED,

    /**
     * 환불 거절
     */
    REJECTED,

    /**
     * 환불 완료
     */
    COMPLETED
}