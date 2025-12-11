package com.company.settlement.exception;

/**
 * 정산 정보를 찾을 수 없을 때 발생하는 예외
 */
public class SettlementNotFoundException extends RuntimeException {

    private final Long settlementId;

    public SettlementNotFoundException(Long settlementId) {
        super("Settlement not found: " + settlementId);
        this.settlementId = settlementId;
    }

    public SettlementNotFoundException(String message) {
        super(message);
        this.settlementId = null;
    }

    public Long getSettlementId() {
        return settlementId;
    }
}
