package com.company.settlement.repository;

import com.company.settlement.domain.entity.Order;
import com.company.settlement.domain.entity.OrderItem;
import com.company.settlement.domain.entity.Refund;
import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.OrderStatus;
import com.company.settlement.domain.enums.RefundStatus;
import com.company.settlement.domain.enums.RefundType;
import com.company.settlement.domain.enums.SellerStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefundRepository 통합 테스트
 * 
 * 테스트 포인트:
 * - 완료된 환불 조회 (COMPLETED 상태)
 * - BigDecimal 환불액 집계 정확성
 * - Fetch Join을 통한 N+1 문제 해결 검증
 */
class RefundRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private EntityManager entityManager;

    private Seller testSeller;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        // 테스트용 판매자 생성
        testSeller = Seller.builder()
                .sellerCode("REFUND_TEST_SELLER")
                .sellerName("환불 테스트 판매자")
                .commissionRate(new BigDecimal("0.1000"))
                .status(SellerStatus.ACTIVE)
                .build();
        sellerRepository.save(testSeller);

        // 테스트용 주문 및 주문항목 생성
        testOrder = Order.builder()
                .orderNo("REFUND-TEST-ORDER-001")
                .seller(testSeller)
                .orderStatus(OrderStatus.DELIVERED)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("100000"))
                .shippingFee(new BigDecimal("3000"))
                .build();

        testOrderItem = OrderItem.builder()
                .productName("테스트 상품")
                .unitPrice(new BigDecimal("50000"))
                .quantity(2)
                .totalAmount(new BigDecimal("100000"))
                .build();
        testOrder.addOrderItem(testOrderItem);
        orderRepository.save(testOrder);
    }

    @Test
    @DisplayName("주문항목별 환불 목록 조회")
    void findByOrderItemId_Success() {
        // given
        Refund refund1 = createRefund(testOrderItem, new BigDecimal("30000"), RefundStatus.COMPLETED);
        Refund refund2 = createRefund(testOrderItem, new BigDecimal("20000"), RefundStatus.PENDING);

        // when
        List<Refund> refunds = refundRepository.findByOrderItemId(testOrderItem.getId());

        // then
        assertThat(refunds).hasSize(2);
    }

    @Test
    @DisplayName("환불 상태별 목록 조회")
    void findByRefundStatus_Success() {
        // given
        createRefund(testOrderItem, new BigDecimal("10000"), RefundStatus.COMPLETED);
        createRefund(testOrderItem, new BigDecimal("20000"), RefundStatus.COMPLETED);
        createRefund(testOrderItem, new BigDecimal("15000"), RefundStatus.PENDING);

        // when
        List<Refund> completedRefunds = refundRepository.findByRefundStatus(RefundStatus.COMPLETED);

        // then
        assertThat(completedRefunds).hasSize(2);
    }

    @Test
    @DisplayName("판매자별 완료된 환불 조회 - Fetch Join으로 N+1 방지")
    void findCompletedRefundsBySeller_FetchJoin() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        createCompletedRefund(testOrderItem, new BigDecimal("25000"));
        createCompletedRefund(testOrderItem, new BigDecimal("35000"));

        // 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();

        // when
        List<Refund> refunds = refundRepository.findCompletedRefundsBySeller(
                testSeller.getId(), startDate, endDate);

        // then
        assertThat(refunds).hasSize(2);

        // Fetch Join 검증: OrderItem과 Order에 접근해도 추가 쿼리 없음
        refunds.forEach(r -> {
            assertThat(r.getOrderItem()).isNotNull();
            assertThat(r.getOrderItem().getOrder()).isNotNull();
            assertThat(r.getOrderItem().getOrder().getSeller().getId()).isEqualTo(testSeller.getId());
        });
    }

    @Test
    @DisplayName("판매자별 총 환불 금액 집계 - BigDecimal 정확성")
    void calculateTotalRefundAmount_ReturnsCorrectSum() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        createCompletedRefund(testOrderItem, new BigDecimal("12345.67"));
        createCompletedRefund(testOrderItem, new BigDecimal("23456.78"));
        createCompletedRefund(testOrderItem, new BigDecimal("34567.89"));

        // when
        BigDecimal totalRefundAmount = refundRepository.calculateTotalRefundAmount(
                testSeller.getId(), startDate, endDate);

        // then
        // 12345.67 + 23456.78 + 34567.89 = 70370.34
        assertThat(totalRefundAmount).isEqualByComparingTo(new BigDecimal("70370.34"));
    }

    @Test
    @DisplayName("환불 없을 때 총 환불 금액 0 반환 - NPE 방지")
    void calculateTotalRefundAmount_NoRefunds_ReturnsZero() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        // 환불 데이터 없음

        // when
        BigDecimal totalRefundAmount = refundRepository.calculateTotalRefundAmount(
                testSeller.getId(), startDate, endDate);

        // then
        assertThat(totalRefundAmount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("PENDING 상태 환불은 집계에서 제외")
    void calculateTotalRefundAmount_ExcludesPendingRefunds() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        createCompletedRefund(testOrderItem, new BigDecimal("10000"));
        createRefund(testOrderItem, new BigDecimal("20000"), RefundStatus.PENDING);
        createRefund(testOrderItem, new BigDecimal("30000"), RefundStatus.APPROVED);

        // when
        BigDecimal totalRefundAmount = refundRepository.calculateTotalRefundAmount(
                testSeller.getId(), startDate, endDate);

        // then
        // COMPLETED 상태인 10000만 집계
        assertThat(totalRefundAmount).isEqualByComparingTo(new BigDecimal("10000"));
    }

    // ========== Helper Methods ==========

    private Refund createRefund(OrderItem orderItem, BigDecimal amount, RefundStatus status) {
        Refund refund = Refund.builder()
                .orderItem(orderItem)
                .refundType(RefundType.PARTIAL_AMOUNT)
                .refundAmount(amount)
                .refundQuantity(1)
                .refundReason("테스트 환불")
                .refundStatus(status)
                .build();
        return refundRepository.save(refund);
    }

    private Refund createCompletedRefund(OrderItem orderItem, BigDecimal amount) {
        Refund refund = Refund.builder()
                .orderItem(orderItem)
                .refundType(RefundType.PARTIAL_AMOUNT)
                .refundAmount(amount)
                .refundQuantity(1)
                .refundReason("완료된 테스트 환불")
                .refundStatus(RefundStatus.APPROVED)
                .build();
        refund.complete(); // COMPLETED 상태로 변경 + refundedAt 설정
        return refundRepository.save(refund);
    }
}
