package com.company.settlement.repository;

import com.company.settlement.domain.entity.Order;
import com.company.settlement.domain.entity.OrderItem;
import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.OrderStatus;
import com.company.settlement.domain.enums.SellerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderRepository 단위 테스트
 * Repository 인터페이스의 메서드 시그니처 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRepository 단위 테스트")
class OrderRepositoryTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    @DisplayName("주문번호로 조회 - 메서드 호출 검증")
    void findByOrderNo_VerifyMethodCall() {
        // given
        String orderNo = "ORD-2024-001";
        Order mockOrder = Order.builder()
                .orderNo(orderNo)
                .orderStatus(OrderStatus.CONFIRMED)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("50000"))
                .build();
        when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(mockOrder));

        // when
        Optional<Order> found = orderRepository.findByOrderNo(orderNo);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        verify(orderRepository).findByOrderNo(orderNo);
    }

    @Test
    @DisplayName("존재하지 않는 주문번호 조회 - 빈 Optional 반환")
    void findByOrderNo_NotFound_ReturnsEmpty() {
        // given
        String orderNo = "NOT_EXIST";
        when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.empty());

        // when
        Optional<Order> found = orderRepository.findByOrderNo(orderNo);

        // then
        assertThat(found).isEmpty();
        verify(orderRepository).findByOrderNo(orderNo);
    }

    @Test
    @DisplayName("정산 대상 주문 조회 - 메서드 호출 검증")
    void findSettlementTargetOrders_VerifyMethodCall() {
        // given
        Long sellerId = 1L;
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        List<Order> mockOrders = List.of(
                createMockOrder("ORD-CONFIRMED", OrderStatus.CONFIRMED),
                createMockOrder("ORD-SHIPPED", OrderStatus.SHIPPED),
                createMockOrder("ORD-DELIVERED", OrderStatus.DELIVERED)
        );
        when(orderRepository.findSettlementTargetOrders(eq(sellerId), eq(startDate), eq(endDate)))
                .thenReturn(mockOrders);

        // when
        List<Order> settlementTargets = orderRepository.findSettlementTargetOrders(sellerId, startDate, endDate);

        // then
        assertThat(settlementTargets).hasSize(3);
        assertThat(settlementTargets)
                .extracting(Order::getOrderNo)
                .containsExactlyInAnyOrder("ORD-CONFIRMED", "ORD-SHIPPED", "ORD-DELIVERED");
        verify(orderRepository).findSettlementTargetOrders(sellerId, startDate, endDate);
    }

    @Test
    @DisplayName("정산 대상 주문+품목 조회 - 메서드 호출 검증")
    void findSettlementTargetOrdersWithItems_VerifyMethodCall() {
        // given
        Long sellerId = 1L;
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        List<Order> mockOrders = List.of(createMockOrderWithItems("ORD-001", OrderStatus.CONFIRMED, 3));
        when(orderRepository.findSettlementTargetOrdersWithItems(eq(sellerId), eq(startDate), eq(endDate)))
                .thenReturn(mockOrders);

        // when
        List<Order> orders = orderRepository.findSettlementTargetOrdersWithItems(sellerId, startDate, endDate);

        // then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderItems()).hasSize(3);
        verify(orderRepository).findSettlementTargetOrdersWithItems(sellerId, startDate, endDate);
    }

    @Test
    @DisplayName("판매자별 주문 수 집계 - 메서드 호출 검증")
    void countBySellerId_VerifyMethodCall() {
        // given
        Long sellerId = 1L;
        when(orderRepository.countBySellerId(sellerId)).thenReturn(3L);

        // when
        long count = orderRepository.countBySellerId(sellerId);

        // then
        assertThat(count).isEqualTo(3);
        verify(orderRepository).countBySellerId(sellerId);
    }

    @Test
    @DisplayName("주문 저장 - 메서드 호출 검증")
    void save_VerifyMethodCall() {
        // given
        Order order = Order.builder()
                .orderNo("ORD-001")
                .orderStatus(OrderStatus.CONFIRMED)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("50000"))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        Order saved = orderRepository.save(order);

        // then
        assertThat(saved.getOrderNo()).isEqualTo("ORD-001");
        verify(orderRepository).save(order);
    }

    private Order createMockOrder(String orderNo, OrderStatus status) {
        return Order.builder()
                .orderNo(orderNo)
                .orderStatus(status)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("30000"))
                .build();
    }

    private Order createMockOrderWithItems(String orderNo, OrderStatus status, int itemCount) {
        Order order = Order.builder()
                .orderNo(orderNo)
                .orderStatus(status)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("100000"))
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
        return order;
    }
}
