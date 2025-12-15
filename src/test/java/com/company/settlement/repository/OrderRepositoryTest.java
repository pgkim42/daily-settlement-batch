package com.company.settlement.repository;

import com.company.settlement.domain.entity.Order;
import com.company.settlement.domain.entity.OrderItem;
import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.OrderStatus;
import com.company.settlement.domain.enums.SellerStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderRepository 통합 테스트
 * 
 * 테스트 포인트:
 * - 정산 대상 주문 필터링 (CONFIRMED, SHIPPED, DELIVERED)
 * - Fetch Join을 통한 N+1 문제 해결 검증
 */
class OrderRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private EntityManager entityManager;

    private Seller testSeller;

    @BeforeEach
    void setUp() {
        // 테스트용 판매자 생성
        testSeller = Seller.builder()
                .sellerCode("ORDER_TEST_SELLER")
                .sellerName("주문 테스트 판매자")
                .commissionRate(new BigDecimal("0.1000"))
                .status(SellerStatus.ACTIVE)
                .build();
        sellerRepository.save(testSeller);
    }

    @Test
    @DisplayName("주문번호로 조회 성공")
    void findByOrderNo_Success() {
        // given
        Order order = Order.builder()
                .orderNo("ORD-2024-001")
                .seller(testSeller)
                .orderStatus(OrderStatus.CONFIRMED)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("50000"))
                .shippingFee(new BigDecimal("3000"))
                .build();
        orderRepository.save(order);

        // when
        Optional<Order> found = orderRepository.findByOrderNo("ORD-2024-001");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("존재하지 않는 주문번호 조회 시 빈 Optional 반환")
    void findByOrderNo_NotFound_ReturnsEmpty() {
        // when
        Optional<Order> found = orderRepository.findByOrderNo("NOT_EXIST");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("정산 대상 주문만 필터링 - CONFIRMED, SHIPPED, DELIVERED 포함")
    void findSettlementTargetOrders_FiltersCorrectStatus() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // 정산 대상: CONFIRMED, SHIPPED, DELIVERED
        createOrder("ORD-CONFIRMED", OrderStatus.CONFIRMED);
        createOrder("ORD-SHIPPED", OrderStatus.SHIPPED);
        createOrder("ORD-DELIVERED", OrderStatus.DELIVERED);

        // 정산 비대상: PENDING, CANCELLED
        createOrder("ORD-PENDING", OrderStatus.PENDING);
        createOrder("ORD-CANCELLED", OrderStatus.CANCELLED);

        // when
        List<Order> settlementTargets = orderRepository.findSettlementTargetOrders(
                testSeller.getId(), startDate, endDate);

        // then
        assertThat(settlementTargets).hasSize(3);
        assertThat(settlementTargets)
                .extracting(Order::getOrderNo)
                .containsExactlyInAnyOrder("ORD-CONFIRMED", "ORD-SHIPPED", "ORD-DELIVERED");
    }

    @Test
    @DisplayName("정산 대상 주문 조회 시 PENDING, CANCELLED 제외")
    void findSettlementTargetOrders_ExcludesPendingAndCancelled() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        createOrder("ORD-PENDING-1", OrderStatus.PENDING);
        createOrder("ORD-CANCELLED-1", OrderStatus.CANCELLED);

        // when
        List<Order> settlementTargets = orderRepository.findSettlementTargetOrders(
                testSeller.getId(), startDate, endDate);

        // then
        assertThat(settlementTargets).isEmpty();
    }

    @Test
    @DisplayName("Fetch Join으로 OrderItems 조회 시 N+1 문제 없음")
    void findSettlementTargetOrdersWithItems_FetchJoin() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        Order order1 = createOrderWithItems("ORD-WITH-ITEMS-1", OrderStatus.CONFIRMED, 3);
        Order order2 = createOrderWithItems("ORD-WITH-ITEMS-2", OrderStatus.DELIVERED, 2);

        // 영속성 컨텍스트 초기화 (캐시 제거)
        entityManager.flush();
        entityManager.clear();

        // when
        List<Order> orders = orderRepository.findSettlementTargetOrdersWithItems(
                testSeller.getId(), startDate, endDate);

        // then
        assertThat(orders).hasSize(2);

        // Fetch Join 검증: 추가 쿼리 없이 OrderItems 접근 가능
        // LazyInitializationException이 발생하지 않으면 성공
        int totalItemCount = orders.stream()
                .mapToInt(o -> o.getOrderItems().size())
                .sum();
        assertThat(totalItemCount).isEqualTo(5); // 3 + 2
    }

    @Test
    @DisplayName("판매자별 주문 수 집계")
    void countBySellerId_ReturnsCorrectCount() {
        // given
        createOrder("ORD-COUNT-1", OrderStatus.CONFIRMED);
        createOrder("ORD-COUNT-2", OrderStatus.SHIPPED);
        createOrder("ORD-COUNT-3", OrderStatus.PENDING);

        // when
        long count = orderRepository.countBySellerId(testSeller.getId());

        // then
        assertThat(count).isEqualTo(3);
    }

    // ========== Helper Methods ==========

    private Order createOrder(String orderNo, OrderStatus status) {
        Order order = Order.builder()
                .orderNo(orderNo)
                .seller(testSeller)
                .orderStatus(status)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("30000"))
                .shippingFee(new BigDecimal("2500"))
                .build();
        return orderRepository.save(order);
    }

    private Order createOrderWithItems(String orderNo, OrderStatus status, int itemCount) {
        Order order = Order.builder()
                .orderNo(orderNo)
                .seller(testSeller)
                .orderStatus(status)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("100000"))
                .shippingFee(new BigDecimal("3000"))
                .build();

        for (int i = 1; i <= itemCount; i++) {
            OrderItem item = OrderItem.builder()
                    .productName("상품 " + i)
                    .unitPrice(new BigDecimal("10000"))
                    .quantity(2)
                    .totalAmount(new BigDecimal("20000"))
                    .build();
            order.addOrderItem(item);
        }

        return orderRepository.save(order);
    }
}
