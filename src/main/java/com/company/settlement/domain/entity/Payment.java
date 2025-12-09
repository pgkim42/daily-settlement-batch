package com.company.settlement.domain.entity;

import com.company.settlement.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 Entity
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "ix_payments_order", columnList = "order_id", unique = true),
    @Index(name = "ix_payments_status", columnList = "payment_status"),
    @Index(name = "ix_payments_paid_at", columnList = "paid_at")
})
@Comment("결제 정보")
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Comment("결제상태")
    private PaymentStatus paymentStatus;

    @Column(name = "payment_method", nullable = false, length = 50)
    @Comment("결제수단")
    private String paymentMethod;

    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    @Comment("결제 금액")
    private BigDecimal paymentAmount;

    @Column(name = "paid_at")
    @Comment("결제 확정일시")
    private LocalDateTime paidAt;

    @Builder
    public Payment(Order order, PaymentStatus paymentStatus, String paymentMethod,
                   BigDecimal paymentAmount, LocalDateTime paidAt) {
        this.order = order;
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.PENDING;
        this.paymentMethod = paymentMethod;
        this.paymentAmount = paymentAmount != null ? paymentAmount : BigDecimal.ZERO;
        this.paidAt = paidAt;
    }

    /**
     * 결제 정보 설정 (Order에서 호출)
     */
    void setOrder(Order order) {
        this.order = order;
    }

    /**
     * 결제 확정
     */
    public void confirm() {
        if (this.paymentStatus == PaymentStatus.PENDING) {
            this.paymentStatus = PaymentStatus.CONFIRMED;
            this.paidAt = LocalDateTime.now();
        }
    }

    /**
     * 결제 실패
     */
    public void fail() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    /**
     * 결제 취소
     */
    public void cancel() {
        if (this.paymentStatus == PaymentStatus.CONFIRMED) {
            this.paymentStatus = PaymentStatus.CANCELLED;
        }
    }

    /**
     * 부분 환불 처리
     */
    public void partialRefund() {
        if (this.paymentStatus == PaymentStatus.CONFIRMED) {
            this.paymentStatus = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }

    /**
     * 전체 환불 처리
     */
    public void fullRefund() {
        if (this.paymentStatus == PaymentStatus.CONFIRMED
            || this.paymentStatus == PaymentStatus.PARTIALLY_REFUNDED) {
            this.paymentStatus = PaymentStatus.FULLY_REFUNDED;
        }
    }

    /**
     * 결제 완료 여부 확인
     */
    public boolean isCompleted() {
        return this.paymentStatus == PaymentStatus.CONFIRMED
            || this.paymentStatus == PaymentStatus.PARTIALLY_REFUNDED
            || this.paymentStatus == PaymentStatus.FULLY_REFUNDED;
    }
}