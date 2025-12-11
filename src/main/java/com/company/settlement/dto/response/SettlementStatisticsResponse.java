package com.company.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 정산 통계 응답 DTO (관리자용)
 */
public record SettlementStatisticsResponse(
    LocalDate periodStart,
    LocalDate periodEnd,
    long totalSettlementCount,
    long pendingCount,
    long confirmedCount,
    long paidCount,
    long cancelledCount,
    BigDecimal totalSalesAmount,
    BigDecimal totalRefundAmount,
    BigDecimal totalCommissionAmount,
    BigDecimal totalVatAmount,
    BigDecimal totalSettlementAmount
) {
    /**
     * 빌더 패턴을 위한 정적 팩토리 메서드
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private long totalSettlementCount;
        private long pendingCount;
        private long confirmedCount;
        private long paidCount;
        private long cancelledCount;
        private BigDecimal totalSalesAmount = BigDecimal.ZERO;
        private BigDecimal totalRefundAmount = BigDecimal.ZERO;
        private BigDecimal totalCommissionAmount = BigDecimal.ZERO;
        private BigDecimal totalVatAmount = BigDecimal.ZERO;
        private BigDecimal totalSettlementAmount = BigDecimal.ZERO;

        public Builder periodStart(LocalDate periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public Builder periodEnd(LocalDate periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public Builder totalSettlementCount(long count) {
            this.totalSettlementCount = count;
            return this;
        }

        public Builder pendingCount(long count) {
            this.pendingCount = count;
            return this;
        }

        public Builder confirmedCount(long count) {
            this.confirmedCount = count;
            return this;
        }

        public Builder paidCount(long count) {
            this.paidCount = count;
            return this;
        }

        public Builder cancelledCount(long count) {
            this.cancelledCount = count;
            return this;
        }

        public Builder totalSalesAmount(BigDecimal amount) {
            this.totalSalesAmount = amount;
            return this;
        }

        public Builder totalRefundAmount(BigDecimal amount) {
            this.totalRefundAmount = amount;
            return this;
        }

        public Builder totalCommissionAmount(BigDecimal amount) {
            this.totalCommissionAmount = amount;
            return this;
        }

        public Builder totalVatAmount(BigDecimal amount) {
            this.totalVatAmount = amount;
            return this;
        }

        public Builder totalSettlementAmount(BigDecimal amount) {
            this.totalSettlementAmount = amount;
            return this;
        }

        public SettlementStatisticsResponse build() {
            return new SettlementStatisticsResponse(
                periodStart, periodEnd, totalSettlementCount,
                pendingCount, confirmedCount, paidCount, cancelledCount,
                totalSalesAmount, totalRefundAmount, totalCommissionAmount,
                totalVatAmount, totalSettlementAmount
            );
        }
    }
}
