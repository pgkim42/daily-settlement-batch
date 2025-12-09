package com.company.settlement.domain.enums;

/**
 * 결제 상태
 */
public enum PaymentStatus {
    /**
     * 결제 대기
     */
    PENDING,

    /**
     * 결제 확정
     */
    CONFIRMED,

    /**
     * 결제 실패
     */
    FAILED,

    /**
     * 결제 취소
     */
    CANCELLED,

    /**
     * 부분 환불
     */
    PARTIALLY_REFUNDED,

    /**
     * 전체 환불
     */
    FULLY_REFUNDED
}