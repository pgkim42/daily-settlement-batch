package com.company.settlement.domain.enums;

/**
 * 주문 상태
 */
public enum OrderStatus {
    /**
     * 주문 대기
     */
    PENDING,

    /**
     * 주문 확정 (결제 완료)
     */
    CONFIRMED,

    /**
     * 배송 중
     */
    SHIPPED,

    /**
     * 배송 완료
     */
    DELIVERED,

    /**
     * 주문 취소
     */
    CANCELLED
}