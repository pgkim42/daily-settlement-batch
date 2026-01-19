package com.company.settlement.batch.processor;

import com.company.settlement.batch.dto.SettlementContext;
import com.company.settlement.batch.exception.SettlementAlreadyExistsException;
import com.company.settlement.batch.service.CommissionCalculator;
import com.company.settlement.batch.support.SettlementTestDataFactory;
import com.company.settlement.domain.entity.*;
import com.company.settlement.domain.enums.*;
import com.company.settlement.repository.OrderRepository;
import com.company.settlement.repository.RefundRepository;
import com.company.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SettlementProcessor 단위 테스트
 *
 * 검증 항목:
 * - 정산 금액 계산 로직 정확성
 * - 멱등성 체크 (이미 정산 존재 시 Skip)
 * - 정산 대상 데이터 없는 경우 처리
 * - 전체/부분 환불 포함 정산
 * - 음수 정산 (환불 > 매출) 처리
 *
 * 참고: CommissionCalculator는 실제 인스턴스를 사용 (순수 계산 로직이므로)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementProcessor 단위 테스트")
class SettlementProcessorTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RefundRepository refundRepository;

    // CommissionCalculator는 실제 인스턴스 사용 (의존성 없는 순수 계산 클래스)
    private final CommissionCalculator commissionCalculator = new CommissionCalculator();

    @InjectMocks
    private SettlementProcessor processor;

    private static final LocalDate TARGET_DATE = LocalDate.of(2025, 1, 15);
    private static final String TARGET_DATE_STRING = "2025-01-15";

    /**
     * 리플렉션으로 @Value 필드 설정 및 실제 CommissionCalculator 주입
     */
    @BeforeEach
    void setUp() throws Exception {
        // @Value 필드 설정 (리플렉션)
        var targetDateField = SettlementProcessor.class.getDeclaredField("targetDateString");
        targetDateField.setAccessible(true);
        targetDateField.set(processor, TARGET_DATE_STRING);

        // 실제 CommissionCalculator 인스턴스 주입 (리플렉션)
        var calculatorField = SettlementProcessor.class.getDeclaredField("commissionCalculator");
        calculatorField.setAccessible(true);
        calculatorField.set(processor, commissionCalculator);

        // @PostConstruct 호출
        processor.init();
    }

    @Nested
    @DisplayName("TC-S001: 정상 판매만 있는 경우")
    class NormalSaleOnly {

        @Test
        @DisplayName("정산 금액이 올바르게 계산되어야 한다")
        void calculateSettlementAmounts_correctly() {
            // Given
            Seller seller = SettlementTestDataFactory.ScenarioS001.seller();
            List<Order> orders = SettlementTestDataFactory.ScenarioS001.orders();
            List<Refund> refunds = SettlementTestDataFactory.ScenarioS001.refunds();

            when(settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                eq(seller.getId()), eq(CycleType.DAILY), eq(TARGET_DATE), eq(TARGET_DATE)
            )).thenReturn(Optional.empty());

            when(orderRepository.findSettlementTargetOrdersWithItems(
                eq(seller.getId()), any(), any()
            )).thenReturn(orders);

            when(refundRepository.findCompletedRefundsBySeller(
                eq(seller.getId()), any(), any()
            )).thenReturn(refunds);

            // When
            SettlementContext result = processor.process(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.settlement().getSeller()).isEqualTo(seller);
            assertThat(result.settlement().getGrossSalesAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.settlement().getRefundAmount()).isEqualByComparingTo("0.00");
            assertThat(result.settlement().getCommissionAmount()).isEqualByComparingTo("1000.00");
            assertThat(result.settlement().getTaxAmount()).isEqualByComparingTo("100.00");
            assertThat(result.settlement().getPayoutAmount()).isEqualByComparingTo("8900.00");
            assertThat(result.settlement().getStatus()).isEqualTo(SettlementStatus.PENDING);

            assertThat(result.getSaleItemCount()).isEqualTo(1);
            assertThat(result.getRefundItemCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("TC-S002: 전체 환불 포함")
    class FullRefund {

        @Test
        @DisplayName("전체 환불이 포함된 정산 금액이 계산되어야 한다")
        void calculateWithFullRefund() {
            // Given
            Seller seller = SettlementTestDataFactory.aSeller();
            Order order = SettlementTestDataFactory.aSettlementTargetOrder(seller);
            OrderItem item = SettlementTestDataFactory.anOrderItem();
            order.addOrderItem(item);

            Refund refund = SettlementTestDataFactory.aCompletedFullRefund(item);

            when(settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                any(), any(), any(), any()
            )).thenReturn(Optional.empty());

            when(orderRepository.findSettlementTargetOrdersWithItems(any(), any(), any()))
                .thenReturn(List.of(order));

            when(refundRepository.findCompletedRefundsBySeller(any(), any(), any()))
                .thenReturn(List.of(refund));

            // When
            SettlementContext result = processor.process(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.settlement().getGrossSalesAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.settlement().getRefundAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.settlement().getCommissionAmount()).isEqualByComparingTo("0.00");
            assertThat(result.settlement().getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(result.settlement().getPayoutAmount()).isEqualByComparingTo("0.00");

            assertThat(result.getSaleItemCount()).isEqualTo(1);
            assertThat(result.getRefundItemCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("TC-S003: 부분 환불 포함")
    class PartialRefund {

        @Test
        @DisplayName("부분 환불이 포함된 정산 금액이 계산되어야 한다")
        void calculateWithPartialRefund() {
            // Given
            Seller seller = SettlementTestDataFactory.aSeller();
            Order order = SettlementTestDataFactory.aSettlementTargetOrder(seller);
            OrderItem item = SettlementTestDataFactory.createOrderItem(1L, "부분환불상품", 2, new BigDecimal("10000"));
            order.addOrderItem(item); // totalAmount = 20000

            Refund refund = SettlementTestDataFactory.aCompletedPartialRefund(item, new BigDecimal("5000"));

            when(settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                any(), any(), any(), any()
            )).thenReturn(Optional.empty());

            when(orderRepository.findSettlementTargetOrdersWithItems(any(), any(), any()))
                .thenReturn(List.of(order));

            when(refundRepository.findCompletedRefundsBySeller(any(), any(), any()))
                .thenReturn(List.of(refund));

            // When
            SettlementContext result = processor.process(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.settlement().getGrossSalesAmount()).isEqualByComparingTo("20000.00");
            assertThat(result.settlement().getRefundAmount()).isEqualByComparingTo("5000.00");
            assertThat(result.settlement().getCommissionAmount()).isEqualByComparingTo("1500.00");
            assertThat(result.settlement().getTaxAmount()).isEqualByComparingTo("150.00");
            assertThat(result.settlement().getPayoutAmount()).isEqualByComparingTo("13350.00");
        }
    }

    @Nested
    @DisplayName("TC-S004: 음수 정산 (환불 > 매출)")
    class NegativeSettlement {

        @Test
        @DisplayName("환불이 매출을 초과하는 경우 음수 정산이 계산되어야 한다")
        void calculateNegativeSettlement() {
            // Given
            Seller seller = SettlementTestDataFactory.aSeller();
            Order order = SettlementTestDataFactory.aSettlementTargetOrder(seller);
            OrderItem item = SettlementTestDataFactory.createOrderItem(1L, "환불초과상품", 1, new BigDecimal("10000"));
            order.addOrderItem(item);

            // 환불이 매출보다 큰 경우 (15000 > 10000)
            Refund refund = SettlementTestDataFactory.createRefund(
                1L, item, RefundType.FULL, new BigDecimal("15000"), RefundStatus.COMPLETED
            );

            when(settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                any(), any(), any(), any()
            )).thenReturn(Optional.empty());

            when(orderRepository.findSettlementTargetOrdersWithItems(any(), any(), any()))
                .thenReturn(List.of(order));

            when(refundRepository.findCompletedRefundsBySeller(any(), any(), any()))
                .thenReturn(List.of(refund));

            // When
            SettlementContext result = processor.process(seller);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.settlement().getGrossSalesAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.settlement().getRefundAmount()).isEqualByComparingTo("15000.00");
            assertThat(result.settlement().getCommissionAmount()).isEqualByComparingTo("-500.00");
            assertThat(result.settlement().getTaxAmount()).isEqualByComparingTo("-50.00");
            assertThat(result.settlement().getPayoutAmount()).isEqualByComparingTo("-4450.00"); // 차감 필요
        }
    }

    @Nested
    @DisplayName("TC-S007: 멱등성 검증")
    class Idempotency {

        @Test
        @DisplayName("이미 정산이 존재하는 경우 SettlementAlreadyExistsException이 발생해야 한다")
        void throwExceptionWhenSettlementExists() {
            // Given
            Seller seller = SettlementTestDataFactory.aSeller();
            Settlement existingSettlement = SettlementTestDataFactory.aDailySettlement(seller, TARGET_DATE);

            when(settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                eq(seller.getId()), eq(CycleType.DAILY), eq(TARGET_DATE), eq(TARGET_DATE)
            )).thenReturn(Optional.of(existingSettlement));

            // When & Then
            assertThatThrownBy(() -> processor.process(seller))
                .isInstanceOf(SettlementAlreadyExistsException.class)
                .hasMessageContaining("Settlement already exists");
        }
    }

    @Nested
    @DisplayName("정산 대상 데이터 없음")
    class NoSettlementData {

        @Test
        @DisplayName("정산 대상 데이터가 없으면 null을 반환해야 한다 (Skip)")
        void returnNullWhenNoData() {
            // Given
            Seller seller = SettlementTestDataFactory.aSeller();

            when(settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                any(), any(), any(), any()
            )).thenReturn(Optional.empty());

            when(orderRepository.findSettlementTargetOrdersWithItems(any(), any(), any()))
                .thenReturn(List.of());

            when(refundRepository.findCompletedRefundsBySeller(any(), any(), any()))
                .thenReturn(List.of());

            // When
            SettlementContext result = processor.process(seller);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("반올림 정확성")
    class RoundingAccuracy {

        @Test
        @DisplayName("소수점 반올림이 정확하게 처리되어야 한다")
        void verifyRoundingAccuracy() {
            // Given - 반올림이 필요한 금액 (10,333.33 * 0.1 = 1,033.333)
            Seller seller = SettlementTestDataFactory.aSeller();
            Order order = SettlementTestDataFactory.aSettlementTargetOrder(seller);
            OrderItem item = SettlementTestDataFactory.createOrderItem(1L, "반올림테스트", 1, new BigDecimal("10333.33"));
            order.addOrderItem(item);

            when(settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                any(), any(), any(), any()
            )).thenReturn(Optional.empty());

            when(orderRepository.findSettlementTargetOrdersWithItems(any(), any(), any()))
                .thenReturn(List.of(order));

            when(refundRepository.findCompletedRefundsBySeller(any(), any(), any()))
                .thenReturn(List.of());

            // When
            SettlementContext result = processor.process(seller);

            // Then - HALF_UP 반올림 검증
            assertThat(result).isNotNull();
            assertThat(result.settlement().getGrossSalesAmount()).isEqualByComparingTo("10333.33");
            assertThat(result.settlement().getCommissionAmount()).isEqualByComparingTo("1033.33"); // 1033.333 -> 1033.33
            assertThat(result.settlement().getTaxAmount()).isEqualByComparingTo("103.33"); // 103.333 -> 103.33
            assertThat(result.settlement().getPayoutAmount()).isEqualByComparingTo("9196.67"); // 10333.33 - 1033.33 - 103.33 = 9196.67
        }
    }
}
