package com.company.settlement.dto.response;

import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.domain.enums.CycleType;
import com.company.settlement.domain.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 상세 조회용 응답 DTO (항목 포함)
 */
public record SettlementDetailResponse(
    Long id,
    SellerSummaryResponse seller,
    CycleType cycleType,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal grossSalesAmount,
    BigDecimal refundAmount,
    BigDecimal netSalesAmount,
    BigDecimal commissionRate,
    BigDecimal commissionAmount,
    BigDecimal taxAmount,
    BigDecimal adjustmentAmount,
    BigDecimal payoutAmount,
    SettlementStatus status,
    List<SettlementItemResponse> items,
    LocalDateTime confirmedAt,
    LocalDateTime paidAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Entity를 Response DTO로 변환
     */
    public static SettlementDetailResponse from(Settlement settlement, List<SettlementItemResponse> items) {
        return new SettlementDetailResponse(
            settlement.getId(),
            SellerSummaryResponse.from(settlement.getSeller()),
            settlement.getCycleType(),
            settlement.getPeriodStart(),
            settlement.getPeriodEnd(),
            settlement.getGrossSalesAmount(),
            settlement.getRefundAmount(),
            settlement.getNetSalesAmount(),
            settlement.getCommissionRate(),
            settlement.getCommissionAmount(),
            settlement.getTaxAmount(),
            settlement.getAdjustmentAmount(),
            settlement.getPayoutAmount(),
            settlement.getStatus(),
            items,
            settlement.getConfirmedAt(),
            settlement.getPaidAt(),
            settlement.getCreatedAt(),
            settlement.getUpdatedAt()
        );
    }
}
