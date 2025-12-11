package com.company.settlement.batch.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 수수료 계산 서비스
 *
 * 금액 계산 규칙:
 * - BigDecimal 사용 (부동소수점 정밀도 문제 방지)
 * - HALF_UP 반올림 (은행 표준)
 * - 소수점 2자리 (원화 기준)
 */
@Component
public class CommissionCalculator {

    private static final int DECIMAL_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");  // 부가세 10%

    /**
     * 수수료 계산
     *
     * @param amount 금액
     * @param commissionRate 수수료율 (0.10 = 10%)
     * @return 수수료 금액
     */
    public BigDecimal calculateCommission(BigDecimal amount, BigDecimal commissionRate) {
        if (amount == null || commissionRate == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(commissionRate)
            .setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * 부가세 계산 (수수료의 10%)
     *
     * @param commissionAmount 수수료 금액
     * @return 부가세 금액
     */
    public BigDecimal calculateTax(BigDecimal commissionAmount) {
        if (commissionAmount == null) {
            return BigDecimal.ZERO;
        }
        return commissionAmount.multiply(TAX_RATE)
            .setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * 순금액 계산 (원금 - 수수료)
     *
     * @param grossAmount 원금
     * @param commissionAmount 수수료
     * @return 순금액
     */
    public BigDecimal calculateNetAmount(BigDecimal grossAmount, BigDecimal commissionAmount) {
        if (grossAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal commission = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
        return grossAmount.subtract(commission)
            .setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * 정산액 계산 (순매출액 - 수수료 - 부가세 + 조정액)
     *
     * @param netSalesAmount 순매출액
     * @param commissionAmount 수수료
     * @param taxAmount 부가세
     * @param adjustmentAmount 조정액
     * @return 최종 정산액
     */
    public BigDecimal calculatePayoutAmount(BigDecimal netSalesAmount, BigDecimal commissionAmount,
                                            BigDecimal taxAmount, BigDecimal adjustmentAmount) {
        BigDecimal net = netSalesAmount != null ? netSalesAmount : BigDecimal.ZERO;
        BigDecimal commission = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        BigDecimal adjustment = adjustmentAmount != null ? adjustmentAmount : BigDecimal.ZERO;

        return net.subtract(commission)
            .subtract(tax)
            .add(adjustment)
            .setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * 금액 합산 (null-safe)
     *
     * @param amounts 합산할 금액들
     * @return 합계
     */
    public BigDecimal sum(BigDecimal... amounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {
            if (amount != null) {
                total = total.add(amount);
            }
        }
        return total.setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * 금액 정규화 (소수점 2자리, HALF_UP)
     *
     * @param amount 금액
     * @return 정규화된 금액
     */
    public BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }
}
