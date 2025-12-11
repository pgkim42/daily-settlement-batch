package com.company.settlement.dto.response;

import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.domain.enums.CycleType;
import com.company.settlement.domain.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정산 목록 조회용 응답 DTO
 */
public record SettlementResponse(
    Long id,
    Long sellerId,
    String sellerCode,
    String sellerName,
    CycleType cycleType,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal grossSalesAmount,
    BigDecimal refundAmount,
    BigDecimal commissionAmount,
    BigDecimal taxAmount,
    BigDecimal payoutAmount,
    SettlementStatus status,
    LocalDateTime createdAt
) {
    /**
     * Entity를 Response DTO로 변환
     */
    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
            settlement.getId(),
            settlement.getSeller().getId(),
            settlement.getSeller().getSellerCode(),
            settlement.getSeller().getSellerName(),
            settlement.getCycleType(),
            settlement.getPeriodStart(),
            settlement.getPeriodEnd(),
            settlement.getGrossSalesAmount(),
            settlement.getRefundAmount(),
            settlement.getCommissionAmount(),
            settlement.getTaxAmount(),
            settlement.getPayoutAmount(),
            settlement.getStatus(),
            settlement.getCreatedAt()
        );
    }
}
