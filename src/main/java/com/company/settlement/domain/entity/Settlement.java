package com.company.settlement.domain.entity;

import com.company.settlement.domain.enums.CycleType;
import com.company.settlement.domain.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 정산 Entity
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "settlements",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_settlement_period",
                            columnNames = {"seller_id", "cycle_type", "period_start", "period_end"}),
           @UniqueConstraint(name = "uk_settlement_active",
                            columnNames = {"seller_id", "cycle_type", "period_start", "period_end", "status"})
       },
       indexes = {
           @Index(name = "ix_settlements_seller", columnList = "seller_id"),
           @Index(name = "ix_settlements_status", columnList = "status"),
           @Index(name = "ix_settlements_period", columnList = "period_start, period_end")
       })
@Comment("판매자 정산 정보")
public class Settlement extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_type", nullable = false)
    @Comment("정산 주기")
    private CycleType cycleType;

    @Column(name = "period_start", nullable = false)
    @Comment("정산 기간 시작일")
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    @Comment("정산 기간 종료일")
    private LocalDate periodEnd;

    @Column(name = "gross_sales_amount", nullable = false, precision = 15, scale = 2)
    @Comment("총 매출액")
    private BigDecimal grossSalesAmount;

    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    @Comment("환불액")
    private BigDecimal refundAmount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    @Comment("적용 수수료율")
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    @Comment("수수료액")
    private BigDecimal commissionAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    @Comment("부가세(수수료의 10%)")
    private BigDecimal taxAmount;

    @Column(name = "adjustment_amount", nullable = false, precision = 15, scale = 2)
    @Comment("조정액")
    private BigDecimal adjustmentAmount;

    @Column(name = "payout_amount", nullable = false, precision = 15, scale = 2)
    @Comment("최종 정산액")
    private BigDecimal payoutAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Comment("정산 상태")
    private SettlementStatus status;

    @Column(name = "confirmed_at")
    @Comment("확정일시")
    private LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    @Comment("지급일시")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    @Comment("취소일시")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    @Comment("취소 사유")
    private String cancelReason;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SettlementItem> settlementItems = new ArrayList<>();

    @Builder
    public Settlement(Seller seller, CycleType cycleType, LocalDate periodStart, LocalDate periodEnd,
                      BigDecimal grossSalesAmount, BigDecimal refundAmount, BigDecimal commissionRate,
                      BigDecimal commissionAmount, BigDecimal taxAmount, BigDecimal adjustmentAmount,
                      BigDecimal payoutAmount, SettlementStatus status) {
        this.seller = seller;
        this.cycleType = cycleType != null ? cycleType : CycleType.DAILY;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.grossSalesAmount = grossSalesAmount != null ? grossSalesAmount : BigDecimal.ZERO;
        this.refundAmount = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        this.commissionRate = commissionRate;
        this.commissionAmount = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
        this.taxAmount = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        this.adjustmentAmount = adjustmentAmount != null ? adjustmentAmount : BigDecimal.ZERO;
        this.payoutAmount = payoutAmount != null ? payoutAmount : BigDecimal.ZERO;
        this.status = status != null ? status : SettlementStatus.PENDING;
    }

    /**
     * 정산 항목 추가
     */
    public void addSettlementItem(SettlementItem settlementItem) {
        settlementItems.add(settlementItem);
        settlementItem.setSettlement(this);
    }

    /**
     * 정산 확정
     */
    public void confirm() {
        if (this.status == SettlementStatus.PENDING) {
            this.status = SettlementStatus.CONFIRMED;
            this.confirmedAt = LocalDateTime.now();
        }
    }

    /**
     * 지급 완료 처리
     */
    public void completePayment() {
        if (this.status == SettlementStatus.CONFIRMED) {
            this.status = SettlementStatus.PAID;
            this.paidAt = LocalDateTime.now();
        }
    }

    /**
     * 정산 취소 (재정산 시)
     */
    public void cancel(String reason) {
        this.status = SettlementStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }

    /**
     * 순매출액 계산 (총 매출액 - 환불액)
     */
    public BigDecimal getNetSalesAmount() {
        return grossSalesAmount.subtract(refundAmount);
    }

    /**
     * 총 차감액 계산 (수수료 + 세금 - 조정액)
     */
    public BigDecimal getTotalDeductionAmount() {
        return commissionAmount
            .add(taxAmount)
            .subtract(adjustmentAmount);
    }

    /**
     * 정산 가능 상태인지 확인
     */
    public boolean isSettleable() {
        return this.status == SettlementStatus.PENDING;
    }

    /**
     * 취소된 정산인지 확인
     */
    public boolean isCancelled() {
        return this.status == SettlementStatus.CANCELLED;
    }
}