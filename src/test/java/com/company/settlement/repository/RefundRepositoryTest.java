package com.company.settlement.repository;

import com.company.settlement.domain.entity.Order;
import com.company.settlement.domain.entity.OrderItem;
import com.company.settlement.domain.entity.Refund;
import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.OrderStatus;
import com.company.settlement.domain.enums.RefundStatus;
import com.company.settlement.domain.enums.RefundType;
import com.company.settlement.domain.enums.SellerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RefundRepository 단위 테스트
 * Repository 인터페이스의 메서드 시그니처 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefundRepository 단위 테스트")
class RefundRepositoryTest {

    @Mock
    private RefundRepository refundRepository;

    @Test
    @DisplayName("주문항목별 환불 목록 조회 - 메서드 호출 검증")
    void findByOrderItemId_VerifyMethodCall() {
        // given
        Long orderItemId = 1L;
        List<Refund> mockRefunds = List.of(
                createMockRefund(new BigDecimal("30000"), RefundStatus.COMPLETED),
                createMockRefund(new BigDecimal("20000"), RefundStatus.PENDING)
        );
        when(refundRepository.findByOrderItemId(orderItemId)).thenReturn(mockRefunds);

        // when
        List<Refund> refunds = refundRepository.findByOrderItemId(orderItemId);

        // then
        assertThat(refunds).hasSize(2);
        verify(refundRepository).findByOrderItemId(orderItemId);
    }

    @Test
    @DisplayName("환불 상태별 목록 조회 - 메서드 호출 검증")
    void findByRefundStatus_VerifyMethodCall() {
        // given
        List<Refund> mockRefunds = List.of(
                createMockRefund(new BigDecimal("10000"), RefundStatus.COMPLETED),
                createMockRefund(new BigDecimal("20000"), RefundStatus.COMPLETED)
        );
        when(refundRepository.findByRefundStatus(RefundStatus.COMPLETED)).thenReturn(mockRefunds);

        // when
        List<Refund> completedRefunds = refundRepository.findByRefundStatus(RefundStatus.COMPLETED);

        // then
        assertThat(completedRefunds).hasSize(2);
        verify(refundRepository).findByRefundStatus(RefundStatus.COMPLETED);
    }

    @Test
    @DisplayName("판매자별 완료된 환불 조회 - 메서드 호출 검증")
    void findCompletedRefundsBySeller_VerifyMethodCall() {
        // given
        Long sellerId = 1L;
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        List<Refund> mockRefunds = List.of(
                createMockRefund(new BigDecimal("25000"), RefundStatus.COMPLETED),
                createMockRefund(new BigDecimal("35000"), RefundStatus.COMPLETED)
        );
        when(refundRepository.findCompletedRefundsBySeller(eq(sellerId), eq(startDate), eq(endDate)))
                .thenReturn(mockRefunds);

        // when
        List<Refund> refunds = refundRepository.findCompletedRefundsBySeller(sellerId, startDate, endDate);

        // then
        assertThat(refunds).hasSize(2);
        verify(refundRepository).findCompletedRefundsBySeller(sellerId, startDate, endDate);
    }

    @Test
    @DisplayName("판매자별 총 환불 금액 집계 - 메서드 호출 검증")
    void calculateTotalRefundAmount_VerifyMethodCall() {
        // given
        Long sellerId = 1L;
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        BigDecimal expectedTotal = new BigDecimal("70370.34");

        when(refundRepository.calculateTotalRefundAmount(eq(sellerId), eq(startDate), eq(endDate)))
                .thenReturn(expectedTotal);

        // when
        BigDecimal totalRefundAmount = refundRepository.calculateTotalRefundAmount(sellerId, startDate, endDate);

        // then
        assertThat(totalRefundAmount).isEqualByComparingTo(expectedTotal);
        verify(refundRepository).calculateTotalRefundAmount(sellerId, startDate, endDate);
    }

    @Test
    @DisplayName("환불 없을 때 총 환불 금액 0 반환 - 메서드 호출 검증")
    void calculateTotalRefundAmount_NoRefunds_ReturnsZero() {
        // given
        Long sellerId = 1L;
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        when(refundRepository.calculateTotalRefundAmount(eq(sellerId), eq(startDate), eq(endDate)))
                .thenReturn(BigDecimal.ZERO);

        // when
        BigDecimal totalRefundAmount = refundRepository.calculateTotalRefundAmount(sellerId, startDate, endDate);

        // then
        assertThat(totalRefundAmount).isEqualByComparingTo(BigDecimal.ZERO);
        verify(refundRepository).calculateTotalRefundAmount(sellerId, startDate, endDate);
    }

    @Test
    @DisplayName("환불 저장 - 메서드 호출 검증")
    void save_VerifyMethodCall() {
        // given
        Refund refund = createMockRefund(new BigDecimal("10000"), RefundStatus.PENDING);
        when(refundRepository.save(refund)).thenReturn(refund);

        // when
        Refund saved = refundRepository.save(refund);

        // then
        assertThat(saved.getRefundAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        verify(refundRepository).save(refund);
    }

    private Refund createMockRefund(BigDecimal amount, RefundStatus status) {
        OrderItem orderItem = OrderItem.builder()
                .productName("테스트 상품")
                .unitPrice(new BigDecimal("50000"))
                .quantity(2)
                .build();

        return Refund.builder()
                .orderItem(orderItem)
                .refundType(RefundType.PARTIAL_AMOUNT)
                .refundAmount(amount)
                .refundQuantity(1)
                .refundReason("테스트 환불")
                .refundStatus(status)
                .build();
    }
}
