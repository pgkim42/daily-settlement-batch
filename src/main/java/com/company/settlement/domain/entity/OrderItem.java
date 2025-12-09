package com.company.settlement.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 항목 Entity
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "ix_order_items_order", columnList = "order_id"),
    @Index(name = "ix_order_items_refunded", columnList = "is_refunded")
})
@Comment("주문 상품 정보")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_name", nullable = false, length = 200)
    @Comment("상품명")
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    @Comment("개별 상품 가격")
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    @Comment("수량")
    private Integer quantity;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Comment("상품별 총액")
    private BigDecimal totalAmount;

    @Column(name = "is_refunded", nullable = false)
    @Comment("환불 여부")
    private Boolean isRefunded;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Refund> refunds = new ArrayList<>();

    @Builder
    public OrderItem(Order order, String productName, BigDecimal unitPrice,
                     Integer quantity, BigDecimal totalAmount) {
        this.order = order;
        this.productName = productName;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.quantity = quantity != null ? quantity : 0;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.isRefunded = false;
    }

    /**
     * 환불 처리
     */
    public void markAsRefunded() {
        this.isRefunded = true;
    }

    /**
     * 환불 항목 추가
     */
    public void addRefund(Refund refund) {
        refunds.add(refund);
        refund.setOrderItem(this);
        markAsRefunded();
    }

    /**
     * 전체 환불 가능 여부
     */
    public boolean isFullyRefundable() {
        return !isRefunded && refunds.isEmpty();
    }

    /**
     * 환불 가능 수량 계산
     */
    public Integer getRefundableQuantity() {
        if (isRefunded) {
            return 0;
        }

        int refundedQuantity = refunds.stream()
            .mapToInt(Refund::getRefundQuantity)
            .sum();

        return quantity - refundedQuantity;
    }

    /**
     * 주문 정보 설정 (Order에서 호출)
     */
    void setOrder(Order order) {
        this.order = order;
    }
}