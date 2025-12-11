package com.company.settlement.service.impl;

import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.domain.enums.SettlementStatus;
import com.company.settlement.dto.response.SettlementDetailResponse;
import com.company.settlement.dto.response.SettlementItemResponse;
import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.dto.response.SettlementStatisticsResponse;
import com.company.settlement.exception.SettlementNotFoundException;
import com.company.settlement.repository.SettlementRepository;
import com.company.settlement.service.AdminSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 관리자용 정산 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminSettlementServiceImpl implements AdminSettlementService {

    private final SettlementRepository settlementRepository;

    @Override
    public Page<SettlementResponse> getAllSettlements(Pageable pageable) {
        log.debug("Getting all settlements with pageable: {}", pageable);

        return settlementRepository.findAllWithSeller(pageable)
            .map(SettlementResponse::from);
    }

    @Override
    public Page<SettlementResponse> getSettlementsByPeriod(LocalDate periodStart, LocalDate periodEnd,
                                                           Pageable pageable) {
        log.debug("Getting settlements by period: {} ~ {}", periodStart, periodEnd);

        return settlementRepository.findByPeriodWithSeller(periodStart, periodEnd, pageable)
            .map(SettlementResponse::from);
    }

    @Override
    public SettlementDetailResponse getSettlementDetail(Long settlementId) {
        log.debug("Getting settlement detail for admin: settlementId={}", settlementId);

        Settlement settlement = settlementRepository.findByIdWithSellerAndItems(settlementId)
            .orElseThrow(() -> new SettlementNotFoundException(settlementId));

        List<SettlementItemResponse> items = settlement.getSettlementItems().stream()
            .map(SettlementItemResponse::from)
            .toList();

        return SettlementDetailResponse.from(settlement, items);
    }

    @Override
    public SettlementStatisticsResponse getStatistics(LocalDate periodStart, LocalDate periodEnd) {
        log.debug("Getting settlement statistics: {} ~ {}", periodStart, periodEnd);

        // 금액 통계 조회
        Object[] amountStats = settlementRepository.getSettlementAmountStatistics(periodStart, periodEnd);

        // 상태별 건수 조회
        long pendingCount = settlementRepository.countByPeriodAndStatus(
            periodStart, periodEnd, SettlementStatus.PENDING);
        long confirmedCount = settlementRepository.countByPeriodAndStatus(
            periodStart, periodEnd, SettlementStatus.CONFIRMED);
        long paidCount = settlementRepository.countByPeriodAndStatus(
            periodStart, periodEnd, SettlementStatus.PAID);
        long cancelledCount = settlementRepository.countByPeriodAndStatus(
            periodStart, periodEnd, SettlementStatus.CANCELLED);

        long totalCount = pendingCount + confirmedCount + paidCount + cancelledCount;

        return SettlementStatisticsResponse.builder()
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .totalSettlementCount(totalCount)
            .pendingCount(pendingCount)
            .confirmedCount(confirmedCount)
            .paidCount(paidCount)
            .cancelledCount(cancelledCount)
            .totalSalesAmount((BigDecimal) amountStats[0])
            .totalRefundAmount((BigDecimal) amountStats[1])
            .totalCommissionAmount((BigDecimal) amountStats[2])
            .totalVatAmount((BigDecimal) amountStats[3])
            .totalSettlementAmount((BigDecimal) amountStats[4])
            .build();
    }
}
