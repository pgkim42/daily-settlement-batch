package com.company.settlement.domain.enums;

/**
 * 정산 항목 원천 타입
 */
public enum SettlementSource {
    /**
     * 주문 항목
     */
    ORDER_ITEM,

    /**
     * 환불
     */
    REFUND,

    /**
     * 수동 입력
     */
    MANUAL
}