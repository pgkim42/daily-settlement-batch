package com.company.settlement.domain.entity;

import com.company.settlement.domain.enums.SettlementItemType;
import com.company.settlement.domain.enums.SettlementSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

/**
 * 정산 항목 Entity
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "settlement_items", indexes = {
    @Index(name = "ix_settlement_items_settlement", columnList = "settlement_id"),
    @Index(name = "ix_settlement_items_type", columnList = "item_type"),
    @Index(name = "ix_settlement_items_source", columnList = "source_type, source_id")
})
@Comment("정산 항목 상세")
public class SettlementItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    @Comment("항목 타입")
    private SettlementItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    @Comment("원천 타입")
    private SettlementSource sourceType;

    @Column(name = "source_id")
    @Comment("원천 데이터 ID (order_item.id 또는 refund.id)")
    private Long sourceId;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    @Comment("매출액/환불 원금")
    private BigDecimal grossAmount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    @Comment("적용 수수료율")
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    @Comment("수수료액")
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    @Comment("순금액 (원금 - 수수료)")
    private BigDecimal netAmount;

    @Column(name = "description", length = 500)
    @Comment("항목 설명 (특히 ADJUSTMENT 타입)")
    private String description;

    @Builder
    public SettlementItem(SettlementItemType itemType, SettlementSource sourceType, Long sourceId,
                          BigDecimal grossAmount, BigDecimal commissionRate, BigDecimal commissionAmount,
                          BigDecimal netAmount, String description) {
        this.itemType = itemType;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.grossAmount = grossAmount != null ? grossAmount : BigDecimal.ZERO;
        this.commissionRate = commissionRate != null ? commissionRate : BigDecimal.ZERO;
        this.commissionAmount = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
        this.netAmount = netAmount != null ? netAmount : BigDecimal.ZERO;
        this.description = description;
    }

    /**
     * 정산 정보 설정 (Settlement에서 호출)
     */
    void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }

    /**
     * 판매 정산 항목 생성
     */
    public static SettlementItem createSaleItem(Long orderItemId, BigDecimal amount,
                                               BigDecimal commissionRate, BigDecimal commissionAmount) {
        return SettlementItem.builder()
            .itemType(SettlementItemType.SALE)
            .sourceType(SettlementSource.ORDER_ITEM)
            .sourceId(orderItemId)
            .grossAmount(amount)
            .commissionRate(commissionRate)
            .commissionAmount(commissionAmount)
            .netAmount(amount.subtract(commissionAmount))
            .description("판매 정산")
            .build();
    }

    /**
     * 환불 정산 항목 생성
     */
    public static SettlementItem createRefundItem(Long refundId, BigDecimal refundAmount,
                                                 BigDecimal commissionRate, BigDecimal commissionAmount) {
        // 환불의 경우 음수로 처리
        BigDecimal negativeRefundAmount = refundAmount.negate();
        BigDecimal negativeCommissionAmount = commissionAmount.negate();

        return SettlementItem.builder()
            .itemType(SettlementItemType.REFUND)
            .sourceType(SettlementSource.REFUND)
            .sourceId(refundId)
            .grossAmount(negativeRefundAmount)
            .commissionRate(commissionRate)
            .commissionAmount(negativeCommissionAmount)
            .netAmount(negativeRefundAmount.add(negativeCommissionAmount))
            .description("환불 차감")
            .build();
    }

    /**
     * 조정 정산 항목 생성
     */
    public static SettlementItem createAdjustmentItem(String description, BigDecimal amount) {
        return SettlementItem.builder()
            .itemType(SettlementItemType.ADJUSTMENT)
            .sourceType(SettlementSource.MANUAL)
            .grossAmount(BigDecimal.ZERO)
            .commissionRate(BigDecimal.ZERO)
            .commissionAmount(BigDecimal.ZERO)
            .netAmount(amount)
            .description(description)
            .build();
    }

    /**
     * 수수료율 기반으로 수수료액 계산
     */
    public void calculateCommission() {
        if (this.grossAmount != null && this.commissionRate != null) {
            this.commissionAmount = this.grossAmount
                .multiply(this.commissionRate)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
            updateNetAmount();
        }
    }

    /**
     * 순금액 업데이트
     */
    private void updateNetAmount() {
        if (this.grossAmount != null && this.commissionAmount != null) {
            this.netAmount = this.grossAmount.subtract(this.commissionAmount);
        }
    }

    /**
     * 환불 항목인지 확인
     */
    public boolean isRefund() {
        return this.itemType == SettlementItemType.REFUND;
    }

    /**
     * 조정 항목인지 확인
     */
    public boolean isAdjustment() {
        return this.itemType == SettlementItemType.ADJUSTMENT;
    }
}