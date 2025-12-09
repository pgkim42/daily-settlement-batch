package com.company.settlement.domain.enums;

/**
 * 환불 유형
 */
public enum RefundType {
    /**
     * 전체 환불
     */
    FULL,

    /**
     * 부분 금액 환불
     */
    PARTIAL_AMOUNT,

    /**
     * 부분 수량 환불
     */
    PARTIAL_QUANTITY
}