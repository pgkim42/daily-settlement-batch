package com.company.settlement.dto.response;

import com.company.settlement.domain.entity.SettlementItem;
import com.company.settlement.domain.enums.SettlementItemType;
import com.company.settlement.domain.enums.SettlementSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 정산 항목 상세 응답 DTO
 */
public record SettlementItemResponse(
    Long id,
    SettlementItemType itemType,
    SettlementSource sourceType,
    Long sourceId,
    BigDecimal grossAmount,
    BigDecimal commissionRate,
    BigDecimal commissionAmount,
    BigDecimal netAmount,
    String description,
    LocalDateTime createdAt
) {
    /**
     * Entity를 Response DTO로 변환
     */
    public static SettlementItemResponse from(SettlementItem item) {
        return new SettlementItemResponse(
            item.getId(),
            item.getItemType(),
            item.getSourceType(),
            item.getSourceId(),
            item.getGrossAmount(),
            item.getCommissionRate(),
            item.getCommissionAmount(),
            item.getNetAmount(),
            item.getDescription(),
            item.getCreatedAt()
        );
    }
}
