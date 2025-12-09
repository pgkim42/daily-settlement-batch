package com.company.settlement.domain.entity;

import com.company.settlement.domain.enums.RefundStatus;
import com.company.settlement.domain.enums.RefundType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환불 Entity
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "ix_refunds_order_item", columnList = "order_item_id"),
    @Index(name = "ix_refunds_status", columnList = "refund_status"),
    @Index(name = "ix_refunds_refunded_at", columnList = "refunded_at")
})
@Comment("환불 정보")
public class Refund extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false)
    @Comment("환불 유형")
    private RefundType refundType;

    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    @Comment("환불 금액")
    private BigDecimal refundAmount;

    @Column(name = "refund_quantity", nullable = false)
    @Comment("환불 수량")
    private Integer refundQuantity;

    @Column(name = "refund_reason", length = 500)
    @Comment("환불 사유")
    private String refundReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false)
    @Comment("환불 상태")
    private RefundStatus refundStatus;

    @Column(name = "refunded_at")
    @Comment("환불 완료일시")
    private LocalDateTime refundedAt;

    @Builder
    public Refund(OrderItem orderItem, RefundType refundType, BigDecimal refundAmount,
                  Integer refundQuantity, String refundReason, RefundStatus refundStatus) {
        this.orderItem = orderItem;
        this.refundType = refundType != null ? refundType : RefundType.FULL;
        this.refundAmount = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        this.refundQuantity = refundQuantity != null ? refundQuantity : 0;
        this.refundReason = refundReason;
        this.refundStatus = refundStatus != null ? refundStatus : RefundStatus.PENDING;
    }

    /**
     * 환불 정보 설정 (OrderItem에서 호출)
     */
    void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    /**
     * 환불 승인
     */
    public void approve() {
        if (this.refundStatus == RefundStatus.PENDING) {
            this.refundStatus = RefundStatus.APPROVED;
        }
    }

    /**
     * 환불 거절
     */
    public void reject() {
        if (this.refundStatus == RefundStatus.PENDING) {
            this.refundStatus = RefundStatus.REJECTED;
        }
    }

    /**
     * 환불 완료 처리
     */
    public void complete() {
        if (this.refundStatus == RefundStatus.APPROVED) {
            this.refundStatus = RefundStatus.COMPLETED;
            this.refundedAt = LocalDateTime.now();
        }
    }

    /**
     * 환불 완료 여부 확인
     */
    public boolean isCompleted() {
        return this.refundStatus == RefundStatus.COMPLETED;
    }

    /**
     * 정산 대상 환불 여부 확인
     */
    public boolean isSettlementTarget() {
        return isCompleted();
    }
}