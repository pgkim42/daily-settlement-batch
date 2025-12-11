package com.company.settlement.batch.exception;

/**
 * 정산 처리 중 오류 발생 시 예외
 * - 정산 계산, 데이터 조회 등의 오류
 */
public class SettlementProcessingException extends RuntimeException {

    public SettlementProcessingException(String message) {
        super(message);
    }

    public SettlementProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
