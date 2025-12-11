package com.company.settlement.dto.response;

import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.SellerStatus;

import java.math.BigDecimal;

/**
 * 판매자 요약 정보 응답 DTO
 */
public record SellerSummaryResponse(
    Long id,
    String sellerCode,
    String sellerName,
    BigDecimal commissionRate,
    SellerStatus status
) {
    /**
     * Entity를 Response DTO로 변환
     */
    public static SellerSummaryResponse from(Seller seller) {
        return new SellerSummaryResponse(
            seller.getId(),
            seller.getSellerCode(),
            seller.getSellerName(),
            seller.getCommissionRate(),
            seller.getStatus()
        );
    }
}
