package com.company.settlement.domain.enums;

/**
 * 판매자 상태
 */
public enum SellerStatus {
    /**
     * 활성 - 정산 가능 상태
     */
    ACTIVE,

    /**
     * 비활성 - 휴면 등
     */
    INACTIVE,

    /**
     * 정지 - 위반 등으로 정산 중단
     */
    SUSPENDED
}