package com.company.settlement.batch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommissionCalculator 단위 테스트
 * 
 * 테스트 포인트:
 * - 수수료 계산 정확성
 * - 부가세 계산 정확성
 * - 정산액 계산 정확성
 * - HALF_UP 반올림 검증
 * - 경계값 테스트 (0, null, 최대값)
 */
class CommissionCalculatorTest {

    private CommissionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CommissionCalculator();
    }

    @Test
    @DisplayName("수수료 계산 - 표준 케이스 (10% 수수료)")
    void calculateCommission_StandardRate() {
        // given
        BigDecimal amount = new BigDecimal("100000");
        BigDecimal commissionRate = new BigDecimal("0.10");

        // when
        BigDecimal commission = calculator.calculateCommission(amount, commissionRate);

        // then
        assertThat(commission).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("수수료 계산 - 소수점 반올림 (HALF_UP)")
    void calculateCommission_RoundingHalfUp() {
        // given
        BigDecimal amount = new BigDecimal("12345.67");
        BigDecimal commissionRate = new BigDecimal("0.1234");

        // when
        BigDecimal commission = calculator.calculateCommission(amount, commissionRate);

        // then
        // 12345.67 * 0.1234 = 1523.455578 -> 1523.46 (HALF_UP)
        assertThat(commission).isEqualByComparingTo(new BigDecimal("1523.46"));
    }

    @Test
    @DisplayName("수수료 계산 - null 입력 시 0 반환")
    void calculateCommission_NullInput_ReturnsZero() {
        // when
        BigDecimal result1 = calculator.calculateCommission(null, new BigDecimal("0.10"));
        BigDecimal result2 = calculator.calculateCommission(new BigDecimal("100000"), null);
        BigDecimal result3 = calculator.calculateCommission(null, null);

        // then
        assertThat(result1).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result2).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result3).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("부가세 계산 - 10% 부가세")
    void calculateTax_TenPercent() {
        // given
        BigDecimal commissionAmount = new BigDecimal("10000");

        // when
        BigDecimal tax = calculator.calculateTax(commissionAmount);

        // then
        assertThat(tax).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("부가세 계산 - 소수점 반올림")
    void calculateTax_RoundingHalfUp() {
        // given
        BigDecimal commissionAmount = new BigDecimal("12345.67");

        // when
        BigDecimal tax = calculator.calculateTax(commissionAmount);

        // then
        // 12345.67 * 0.10 = 1234.567 -> 1234.57 (HALF_UP)
        assertThat(tax).isEqualByComparingTo(new BigDecimal("1234.57"));
    }

    @Test
    @DisplayName("부가세 계산 - null 입력 시 0 반환")
    void calculateTax_NullInput_ReturnsZero() {
        // when
        BigDecimal tax = calculator.calculateTax(null);

        // then
        assertThat(tax).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("순금액 계산 - 원금에서 수수료 차감")
    void calculateNetAmount_Success() {
        // given
        BigDecimal grossAmount = new BigDecimal("100000");
        BigDecimal commissionAmount = new BigDecimal("10000");

        // when
        BigDecimal netAmount = calculator.calculateNetAmount(grossAmount, commissionAmount);

        // then
        assertThat(netAmount).isEqualByComparingTo(new BigDecimal("90000.00"));
    }

    @Test
    @DisplayName("순금액 계산 - null 수수료는 0으로 처리")
    void calculateNetAmount_NullCommission_TreatsAsZero() {
        // given
        BigDecimal grossAmount = new BigDecimal("100000");

        // when
        BigDecimal netAmount = calculator.calculateNetAmount(grossAmount, null);

        // then
        assertThat(netAmount).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("순금액 계산 - null 원금은 0 반환")
    void calculateNetAmount_NullGrossAmount_ReturnsZero() {
        // when
        BigDecimal netAmount = calculator.calculateNetAmount(null, new BigDecimal("10000"));

        // then
        assertThat(netAmount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("정산액 계산 - 표준 케이스")
    void calculatePayoutAmount_StandardCase() {
        // given
        BigDecimal netSalesAmount = new BigDecimal("90000");
        BigDecimal commissionAmount = new BigDecimal("10000");
        BigDecimal taxAmount = new BigDecimal("1000");
        BigDecimal adjustmentAmount = new BigDecimal("500");

        // when
        BigDecimal payoutAmount = calculator.calculatePayoutAmount(
            netSalesAmount, commissionAmount, taxAmount, adjustmentAmount
        );

        // then
        // 90000 - 10000 - 1000 + 500 = 79500
        assertThat(payoutAmount).isEqualByComparingTo(new BigDecimal("79500.00"));
    }

    @Test
    @DisplayName("정산액 계산 - 조정액이 음수인 경우")
    void calculatePayoutAmount_NegativeAdjustment() {
        // given
        BigDecimal netSalesAmount = new BigDecimal("90000");
        BigDecimal commissionAmount = new BigDecimal("10000");
        BigDecimal taxAmount = new BigDecimal("1000");
        BigDecimal adjustmentAmount = new BigDecimal("-2000");

        // when
        BigDecimal payoutAmount = calculator.calculatePayoutAmount(
            netSalesAmount, commissionAmount, taxAmount, adjustmentAmount
        );

        // then
        // 90000 - 10000 - 1000 - 2000 = 77000
        assertThat(payoutAmount).isEqualByComparingTo(new BigDecimal("77000.00"));
    }

    @Test
    @DisplayName("정산액 계산 - null 값들은 0으로 처리")
    void calculatePayoutAmount_NullValues_TreatedAsZero() {
        // given
        BigDecimal netSalesAmount = new BigDecimal("100000");

        // when
        BigDecimal payoutAmount = calculator.calculatePayoutAmount(
            netSalesAmount, null, null, null
        );

        // then
        assertThat(payoutAmount).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("금액 합산 - 여러 금액 합계")
    void sum_MultipleAmounts() {
        // given
        BigDecimal amount1 = new BigDecimal("10000.50");
        BigDecimal amount2 = new BigDecimal("20000.75");
        BigDecimal amount3 = new BigDecimal("30000.25");

        // when
        BigDecimal total = calculator.sum(amount1, amount2, amount3);

        // then
        assertThat(total).isEqualByComparingTo(new BigDecimal("60001.50"));
    }

    @Test
    @DisplayName("금액 합산 - null 값 무시")
    void sum_IgnoresNullValues() {
        // given
        BigDecimal amount1 = new BigDecimal("10000");
        BigDecimal amount2 = null;
        BigDecimal amount3 = new BigDecimal("20000");

        // when
        BigDecimal total = calculator.sum(amount1, amount2, amount3);

        // then
        assertThat(total).isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    @Test
    @DisplayName("금액 합산 - 빈 배열은 0 반환")
    void sum_EmptyArray_ReturnsZero() {
        // when
        BigDecimal total = calculator.sum();

        // then
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("금액 정규화 - 소수점 2자리로 반올림")
    void normalize_RoundsToTwoDecimals() {
        // given
        BigDecimal amount = new BigDecimal("12345.6789");

        // when
        BigDecimal normalized = calculator.normalize(amount);

        // then
        assertThat(normalized).isEqualByComparingTo(new BigDecimal("12345.68"));
    }

    @Test
    @DisplayName("금액 정규화 - null 입력 시 0 반환")
    void normalize_NullInput_ReturnsZero() {
        // when
        BigDecimal normalized = calculator.normalize(null);

        // then
        assertThat(normalized).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @DisplayName("경계값 테스트 - 다양한 금액과 수수료율")
    @CsvSource({
        "0, 0.10, 0.00",
        "1, 0.10, 0.10",
        "100, 0.05, 5.00",
        "999999.99, 0.10, 100000.00",
        "12345.67, 0.1234, 1523.46"
    })
    void calculateCommission_EdgeCases(String amount, String rate, String expected) {
        // given
        BigDecimal amountValue = new BigDecimal(amount);
        BigDecimal rateValue = new BigDecimal(rate);
        BigDecimal expectedValue = new BigDecimal(expected);

        // when
        BigDecimal commission = calculator.calculateCommission(amountValue, rateValue);

        // then
        assertThat(commission).isEqualByComparingTo(expectedValue);
    }

    @ParameterizedTest
    @DisplayName("반올림 정확성 테스트 - HALF_UP 검증")
    @CsvSource({
        "10.004, 10.00",  // 4 -> 내림
        "10.005, 10.01",  // 5 -> 올림 (HALF_UP)
        "10.006, 10.01",  // 6 -> 올림
        "10.994, 10.99",  // 4 -> 내림
        "10.995, 11.00",  // 5 -> 올림 (HALF_UP)
        "10.996, 11.00"   // 6 -> 올림
    })
    void normalize_HalfUpRounding(String input, String expected) {
        // given
        BigDecimal inputValue = new BigDecimal(input);
        BigDecimal expectedValue = new BigDecimal(expected);

        // when
        BigDecimal normalized = calculator.normalize(inputValue);

        // then
        assertThat(normalized).isEqualByComparingTo(expectedValue);
    }

    @Test
    @DisplayName("실제 정산 시나리오 - 전체 계산 흐름")
    void realSettlementScenario() {
        // given - 실제 정산 데이터
        BigDecimal grossSalesAmount = new BigDecimal("1234567.89");  // 총 매출
        BigDecimal refundAmount = new BigDecimal("123456.78");       // 환불액
        BigDecimal commissionRate = new BigDecimal("0.10");          // 10% 수수료

        // when - 정산 계산
        BigDecimal netSalesAmount = grossSalesAmount.subtract(refundAmount);  // 순매출
        BigDecimal commissionAmount = calculator.calculateCommission(netSalesAmount, commissionRate);
        BigDecimal taxAmount = calculator.calculateTax(commissionAmount);
        BigDecimal payoutAmount = calculator.calculatePayoutAmount(
            netSalesAmount, commissionAmount, taxAmount, BigDecimal.ZERO
        );

        // then - 검증
        assertThat(netSalesAmount).isEqualByComparingTo(new BigDecimal("1111111.11"));
        assertThat(commissionAmount).isEqualByComparingTo(new BigDecimal("111111.11"));
        assertThat(taxAmount).isEqualByComparingTo(new BigDecimal("11111.11"));
        assertThat(payoutAmount).isEqualByComparingTo(new BigDecimal("988888.89"));
    }
}
