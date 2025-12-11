package com.company.settlement.exception;

/**
 * 정산 정보에 대한 접근 권한이 없을 때 발생하는 예외
 */
public class SettlementAccessDeniedException extends RuntimeException {

    private final Long sellerId;
    private final Long settlementId;

    public SettlementAccessDeniedException(Long sellerId, Long settlementId) {
        super(String.format("Seller %d does not have access to settlement %d", sellerId, settlementId));
        this.sellerId = sellerId;
        this.settlementId = settlementId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public Long getSettlementId() {
        return settlementId;
    }
}
