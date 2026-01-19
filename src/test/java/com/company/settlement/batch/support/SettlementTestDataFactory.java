package com.company.settlement.batch.support;

import com.company.settlement.domain.entity.*;
import com.company.settlement.domain.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 정산 테스트 데이터 생성 팩토리
 *
 * 테스트에 사용하는 도메인 엔티티 생성을 중앙화하여:
 * - 코드 중복 제거
 * - 일관된 테스트 데이터 제공
 * - 테스트 가독성 향상
 */
public final class SettlementTestDataFactory {

    private SettlementTestDataFactory() {
        // 유틸리티 클래스: 인스턴스화 방지
    }

    // ==================== Seller ====================

    /**
     * 기본 판매자 생성
     */
    public static Seller createSeller(Long id, String code, String name) {
        return createSeller(id, code, name, new BigDecimal("0.1000"));
    }

    /**
     * 수수료율 포함 판매자 생성
     */
    public static Seller createSeller(Long id, String code, String name, BigDecimal commissionRate) {
        Seller seller = Seller.builder()
            .sellerCode(code)
            .sellerName(name)
            .commissionRate(commissionRate)
            .status(SellerStatus.ACTIVE)
            .build();
        setId(seller, id);
        return seller;
    }

    /**
     * 활성 판매자 (기본값)
     */
    public static Seller aSeller() {
        return createSeller(1L, "SELLER001", "테스트판매자");
    }

    /**
     * 수수료율 5% 판매자
     */
    public static Seller aSellerWith5Percent() {
        return createSeller(2L, "SELLER002", "수수료5%판매자", new BigDecimal("0.0500"));
    }

    /**
     * 수수료율 15% 판매자
     */
    public static Seller aSellerWith15Percent() {
        return createSeller(3L, "SELLER003", "수수료15%판매자", new BigDecimal("0.1500"));
    }

    // ==================== OrderItem ====================

    /**
     * 주문 항목 생성
     */
    public static OrderItem createOrderItem(Long id, String productName, int quantity, BigDecimal unitPrice) {
        OrderItem item = OrderItem.builder()
            .productName(productName)
            .unitPrice(unitPrice)
            .quantity(quantity)
            .totalAmount(unitPrice.multiply(new BigDecimal(quantity)))
            .build();
        setId(item, id);
        return item;
    }

    /**
     * 기본 주문 항목
     */
    public static OrderItem anOrderItem() {
        return createOrderItem(1L, "테스트상품", 1, new BigDecimal("10000"));
    }

    /**
     * 다수 주문 항목 생성
     */
    public static List<OrderItem> createOrderItems(Long idStart, String... productNames) {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < productNames.length; i++) {
            items.add(createOrderItem(idStart + i, productNames[i], 1, new BigDecimal("10000")));
        }
        return items;
    }

    // ==================== Order ====================

    /**
     * 주문 생성
     */
    public static Order createOrder(Long id, String orderNo, Seller seller, OrderStatus status, LocalDateTime orderDate) {
        Order order = Order.builder()
            .orderNo(orderNo)
            .seller(seller)
            .orderStatus(status)
            .orderDate(orderDate)
            .totalAmount(BigDecimal.ZERO)
            .shippingFee(BigDecimal.ZERO)
            .build();
        setId(order, id);
        // Lombok @Builder가 필드 초기화를 무시하므로 리플렉션으로 초기화
        initOrderItems(order);
        return order;
    }

    /**
     * 정산 대상 주문 (CONFIRMED 상태)
     */
    public static Order aSettlementTargetOrder(Seller seller) {
        return createOrder(1L, "ORD001", seller, OrderStatus.CONFIRMED, LocalDateTime.now());
    }

    /**
     * 배송 완료 주문
     */
    public static Order aDeliveredOrder(Seller seller) {
        return createOrder(2L, "ORD002", seller, OrderStatus.DELIVERED, LocalDateTime.now());
    }

    /**
     * 주문 + 주문항목 연결
     */
    public static Order createOrderWithItems(Long id, String orderNo, Seller seller, OrderStatus status, List<OrderItem> items) {
        Order order = createOrder(id, orderNo, seller, status, LocalDateTime.now());
        BigDecimal totalAmount = items.stream()
            .map(OrderItem::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (OrderItem item : items) {
            order.addOrderItem(item);
        }
        return order;
    }

    // ==================== Refund ====================

    /**
     * 환불 생성
     */
    public static Refund createRefund(Long id, OrderItem orderItem, RefundType type, BigDecimal amount, RefundStatus status) {
        Refund refund = Refund.builder()
            .orderItem(orderItem)
            .refundType(type)
            .refundAmount(amount)
            .refundQuantity(1)
            .refundStatus(status)
            .build();
        if (status == RefundStatus.COMPLETED) {
            refund.complete();
        }
        setId(refund, id);
        return refund;
    }

    /**
     * 완료된 전체 환불
     */
    public static Refund aCompletedFullRefund(OrderItem orderItem) {
        return createRefund(1L, orderItem, RefundType.FULL, orderItem.getTotalAmount(), RefundStatus.COMPLETED);
    }

    /**
     * 완료된 부분 환불
     */
    public static Refund aCompletedPartialRefund(OrderItem orderItem, BigDecimal amount) {
        return createRefund(2L, orderItem, RefundType.PARTIAL_AMOUNT, amount, RefundStatus.COMPLETED);
    }

    /**
     * 대기 중인 환불 (정산 제외)
     */
    public static Refund aPendingRefund(OrderItem orderItem) {
        return createRefund(3L, orderItem, RefundType.FULL, orderItem.getTotalAmount(), RefundStatus.PENDING);
    }

    // ==================== Settlement ====================

    /**
     * 정산 생성
     */
    public static Settlement createSettlement(Long id, Seller seller, CycleType cycleType,
                                              LocalDate periodStart, LocalDate periodEnd,
                                              BigDecimal grossSalesAmount, BigDecimal refundAmount,
                                              BigDecimal commissionRate, BigDecimal payoutAmount) {
        Settlement settlement = Settlement.builder()
            .seller(seller)
            .cycleType(cycleType)
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .grossSalesAmount(grossSalesAmount)
            .refundAmount(refundAmount)
            .commissionRate(commissionRate)
            .commissionAmount(BigDecimal.ZERO)
            .taxAmount(BigDecimal.ZERO)
            .adjustmentAmount(BigDecimal.ZERO)
            .payoutAmount(payoutAmount)
            .status(SettlementStatus.PENDING)
            .build();
        setId(settlement, id);
        return settlement;
    }

    /**
     * 기본 일일 정산
     */
    public static Settlement aDailySettlement(Seller seller, LocalDate targetDate) {
        return createSettlement(1L, seller, CycleType.DAILY, targetDate, targetDate,
            new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("0.1000"), new BigDecimal("9000"));
    }

    /**
     * 환불 포함 정산
     */
    public static Settlement aSettlementWithRefund(Seller seller, LocalDate targetDate) {
        return createSettlement(2L, seller, CycleType.DAILY, targetDate, targetDate,
            new BigDecimal("10000"), new BigDecimal("3000"), new BigDecimal("0.1000"), new BigDecimal("6300"));
    }

    // ==================== SettlementItem ====================

    /**
     * 판매 정산 항목
     */
    public static SettlementItem aSaleSettlementItem(Long sourceId, BigDecimal amount, BigDecimal commissionRate) {
        BigDecimal commission = amount.multiply(commissionRate).setScale(2, java.math.RoundingMode.HALF_UP);
        return SettlementItem.createSaleItem(sourceId, amount, commissionRate, commission);
    }

    /**
     * 환불 정산 항목
     */
    public static SettlementItem aRefundSettlementItem(Long sourceId, BigDecimal amount, BigDecimal commissionRate) {
        BigDecimal commission = amount.multiply(commissionRate).setScale(2, java.math.RoundingMode.HALF_UP);
        return SettlementItem.createRefundItem(sourceId, amount, commissionRate, commission);
    }

    // ==================== 테스트 시나리오별 데이터 조합 ====================

    /**
     * TC-S001: 정상 판매만 있는 경우
     */
    public static class ScenarioS001 {
        public static final Seller SELLER = aSeller();
        public static final Order ORDER = aSettlementTargetOrder(SELLER);
        public static final OrderItem ITEM = anOrderItem();

        static {
            ORDER.addOrderItem(ITEM);
        }

        public static Seller seller() { return SELLER; }
        public static Order order() { return ORDER; }
        public static List<Order> orders() { return List.of(ORDER); }
        public static List<Refund> refunds() { return List.of(); }
    }

    /**
     * TC-S002: 전체 환불 포함
     */
    public static class ScenarioS002 {
        public static final Seller SELLER = aSeller();
        public static final Order ORDER = aSettlementTargetOrder(SELLER);
        public static final OrderItem ITEM = anOrderItem();
        public static final Refund REFUND = aCompletedFullRefund(ITEM);

        static {
            ORDER.addOrderItem(ITEM);
            ITEM.addRefund(REFUND);
        }

        public static Seller seller() { return SELLER; }
        public static Order order() { return ORDER; }
        public static Refund refund() { return REFUND; }
        public static List<Order> orders() { return List.of(ORDER); }
        public static List<Refund> refunds() { return List.of(REFUND); }
    }

    /**
     * TC-S003: 부분 환불 포함
     */
    public static class ScenarioS003 {
        public static final Seller SELLER = aSeller();
        public static final Order ORDER = aSettlementTargetOrder(SELLER);
        public static final OrderItem ITEM = createOrderItem(1L, "부분환불상품", 2, new BigDecimal("10000"));
        public static final Refund REFUND = aCompletedPartialRefund(ITEM, new BigDecimal("5000"));

        static {
            ORDER.addOrderItem(ITEM);
            ITEM.addRefund(REFUND);
        }

        public static Seller seller() { return SELLER; }
        public static Order order() { return ORDER; }
        public static Refund refund() { return REFUND; }
        public static List<Order> orders() { return List.of(ORDER); }
        public static List<Refund> refunds() { return List.of(REFUND); }
    }

    /**
     * TC-S004: 음수 정산 (환불 > 매출)
     */
    public static class ScenarioS004 {
        public static final Seller SELLER = aSeller();
        public static final Order ORDER = aSettlementTargetOrder(SELLER);
        public static final OrderItem ITEM = createOrderItem(1L, "환불초과상품", 1, new BigDecimal("10000"));
        public static final Refund REFUND = createRefund(1L, ITEM, RefundType.FULL, new BigDecimal("15000"), RefundStatus.COMPLETED);

        static {
            ORDER.addOrderItem(ITEM);
            ITEM.addRefund(REFUND);
        }

        public static Seller seller() { return SELLER; }
        public static Order order() { return ORDER; }
        public static Refund refund() { return REFUND; }
        public static List<Order> orders() { return List.of(ORDER); }
        public static List<Refund> refunds() { return List.of(REFUND); }
    }

    // ==================== 유틸리티 ====================

    /**
     * 리플렉션으로 ID 설정 (테스트용)
     * JPA @Id 필드는 protected 접근자로 설정되어 직접 설정 불가
     */
    @SuppressWarnings("unchecked")
    private static <T> void setId(T entity, Long id) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }

    /**
     * 리플렉션으로 Order의 orderItems 필드 초기화
     * Lombok @Builder가 필드 초기화(= new ArrayList<>())를 무시하므로 필요
     */
    private static void initOrderItems(Order order) {
        try {
            var field = Order.class.getDeclaredField("orderItems");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<OrderItem> items = (List<OrderItem>) field.get(order);
            if (items == null) {
                field.set(order, new ArrayList<OrderItem>());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize orderItems via reflection", e);
        }
    }

    /**
     * 리플렉션으로 생성된At 설정
     */
    @SuppressWarnings("unchecked")
    private static <T> void setCreatedAt(T entity, LocalDateTime createdAt) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt via reflection", e);
        }
    }
}
