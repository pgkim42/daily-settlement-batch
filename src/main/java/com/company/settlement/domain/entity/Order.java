package com.company.settlement.domain.entity;

import com.company.settlement.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 Entity
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "ix_orders_seller", columnList = "seller_id"),
    @Index(name = "ix_orders_status", columnList = "order_status"),
    @Index(name = "ix_orders_date", columnList = "order_date")
})
@Comment("주문 정보")
public class Order extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true, length = 100)
    @Comment("주문번호")
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    @Comment("주문상태")
    private OrderStatus orderStatus;

    @Column(name = "order_date", nullable = false)
    @Comment("주문일시")
    private LocalDateTime orderDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Comment("총 주문 금액")
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    @Comment("배송비")
    private BigDecimal shippingFee;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @Builder
    public Order(String orderNo, Seller seller, OrderStatus orderStatus,
                 LocalDateTime orderDate, BigDecimal totalAmount, BigDecimal shippingFee) {
        this.orderNo = orderNo;
        this.seller = seller;
        this.orderStatus = orderStatus != null ? orderStatus : OrderStatus.PENDING;
        this.orderDate = orderDate != null ? orderDate : LocalDateTime.now();
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.shippingFee = shippingFee != null ? shippingFee : BigDecimal.ZERO;
    }

    /**
     * 주문 상태 변경
     */
    public void changeStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
    }

    /**
     * 주문 확정 (결제 완료 시)
     */
    public void confirm() {
        if (this.orderStatus == OrderStatus.PENDING) {
            this.orderStatus = OrderStatus.CONFIRMED;
        }
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        if (this.orderStatus != OrderStatus.DELIVERED) {
            this.orderStatus = OrderStatus.CANCELLED;
        }
    }

    /**
     * 주문 항목 추가
     */
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    /**
     * 결제 정보 연결
     */
    public void setPayment(Payment payment) {
        this.payment = payment;
        payment.setOrder(this);
    }

    /**
     * 정산 대상 여부 확인
     */
    public boolean isSettlementTarget() {
        return this.orderStatus == OrderStatus.CONFIRMED
            || this.orderStatus == OrderStatus.SHIPPED
            || this.orderStatus == OrderStatus.DELIVERED;
    }
}