package com.company.settlement.batch.exception;

/**
 * 이미 정산이 존재하는 경우 발생하는 예외
 * - 멱등성 보장을 위해 동일 기간 중복 정산 방지
 */
public class SettlementAlreadyExistsException extends RuntimeException {

    public SettlementAlreadyExistsException(String message) {
        super(message);
    }

    public SettlementAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
