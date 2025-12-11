package com.company.settlement.batch.processor;

import com.company.settlement.batch.dto.SettlementContext;
import com.company.settlement.batch.exception.SettlementAlreadyExistsException;
import com.company.settlement.batch.exception.SettlementProcessingException;
import com.company.settlement.batch.service.CommissionCalculator;
import com.company.settlement.domain.entity.*;
import com.company.settlement.domain.enums.CycleType;
import com.company.settlement.domain.enums.SettlementStatus;
import com.company.settlement.repository.OrderRepository;
import com.company.settlement.repository.RefundRepository;
import com.company.settlement.repository.SettlementRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 정산 계산 Processor
 *
 * 핵심 비즈니스 로직:
 * - 멱등성 체크
 * - 정산 대상 주문/환불 조회
 * - 정산 금액 계산
 * - Settlement + SettlementItem 생성
 */
@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class SettlementProcessor implements ItemProcessor<Seller, SettlementContext> {

    private final SettlementRepository settlementRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final CommissionCalculator commissionCalculator;

    @Value("#{jobParameters['targetDate']}")
    private String targetDateString;

    private LocalDate targetDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @PostConstruct
    public void init() {
        this.targetDate = LocalDate.parse(targetDateString);
        // 일일 정산: 당일 00:00:00 ~ 당일 23:59:59
        this.periodStart = targetDate;
        this.periodEnd = targetDate;
    }

    /**
     * 판매자별 정산 계산
     *
     * @param seller 정산 대상 판매자
     * @return SettlementContext (Settlement + SettlementItems) 또는 null (Skip 대상)
     * @throws SettlementAlreadyExistsException 이미 정산이 존재하는 경우
     * @throws SettlementProcessingException 정산 계산 중 오류 발생 시
     */
    @Override
    public SettlementContext process(Seller seller) {
        log.info("[SettlementProcessor] Processing seller: {} ({})",
                 seller.getSellerName(), seller.getSellerCode());

        try {
            // 1. 멱등성 체크 - 이미 정산이 존재하면 Skip
            checkIdempotency(seller);

            // 2. 정산 대상 데이터 조회
            List<Order> orders = fetchSettlementTargetOrders(seller);
            List<Refund> refunds = fetchCompletedRefunds(seller);

            // 3. 정산 대상 데이터가 없으면 null 반환 (Skip)
            if (orders.isEmpty() && refunds.isEmpty()) {
                log.info("[SettlementProcessor] No settlement target data for seller: {}",
                         seller.getSellerCode());
                return null;
            }

            // 4. 정산 금액 계산
            SettlementAmounts amounts = calculateAmounts(orders, refunds, seller.getCommissionRate());

            // 5. Settlement Entity 생성
            Settlement settlement = createSettlement(seller, amounts);

            // 6. SettlementItem 목록 생성
            List<SettlementItem> items = createSettlementItems(orders, refunds, seller.getCommissionRate());

            log.info("[SettlementProcessor] Calculated settlement for {}: grossSales={}, refund={}, commission={}, tax={}, payout={}",
                     seller.getSellerCode(),
                     amounts.grossSalesAmount(),
                     amounts.refundAmount(),
                     amounts.commissionAmount(),
                     amounts.taxAmount(),
                     amounts.payoutAmount());

            return new SettlementContext(settlement, items);

        } catch (SettlementAlreadyExistsException e) {
            log.warn("[SettlementProcessor] Settlement already exists for seller: {}",
                     seller.getSellerCode());
            throw e;  // SkipListener에서 처리
        } catch (Exception e) {
            log.error("[SettlementProcessor] Error processing seller: {}",
                      seller.getSellerCode(), e);
            throw new SettlementProcessingException(
                "Failed to process settlement for seller: " + seller.getSellerCode(), e);
        }
    }

    /**
     * 멱등성 체크: 동일 기간 정산 존재 여부 확인
     */
    private void checkIdempotency(Seller seller) {
        settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
            seller.getId(), CycleType.DAILY, periodStart, periodEnd
        ).ifPresent(existing -> {
            throw new SettlementAlreadyExistsException(
                String.format("Settlement already exists: sellerId=%d, period=%s~%s, status=%s",
                    seller.getId(), periodStart, periodEnd, existing.getStatus())
            );
        });
    }

    /**
     * 정산 대상 주문 조회
     */
    private List<Order> fetchSettlementTargetOrders(Seller seller) {
        LocalDateTime startDateTime = periodStart.atStartOfDay();
        LocalDateTime endDateTime = periodEnd.atTime(23, 59, 59);

        return orderRepository.findSettlementTargetOrdersWithItems(
            seller.getId(), startDateTime, endDateTime
        );
    }

    /**
     * 완료된 환불 조회
     */
    private List<Refund> fetchCompletedRefunds(Seller seller) {
        LocalDateTime startDateTime = periodStart.atStartOfDay();
        LocalDateTime endDateTime = periodEnd.atTime(23, 59, 59);

        return refundRepository.findCompletedRefundsBySeller(
            seller.getId(), startDateTime, endDateTime
        );
    }

    /**
     * 정산 금액 계산
     *
     * 공식:
     * - 순매출액 = 총매출액 - 환불액
     * - 수수료 = 순매출액 x 수수료율
     * - 부가세 = 수수료 x 10%
     * - 정산액 = 순매출액 - 수수료 - 부가세 + 조정액
     */
    private SettlementAmounts calculateAmounts(List<Order> orders, List<Refund> refunds,
                                                BigDecimal commissionRate) {
        // 총매출액: 주문 항목들의 합계
        BigDecimal grossSalesAmount = commissionCalculator.sum(
            orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItem::getTotalAmount)
                .toArray(BigDecimal[]::new)
        );

        // 환불액: 완료된 환불 합계
        BigDecimal refundAmount = commissionCalculator.sum(
            refunds.stream()
                .map(Refund::getRefundAmount)
                .toArray(BigDecimal[]::new)
        );

        // 순매출액
        BigDecimal netSalesAmount = commissionCalculator.calculateNetAmount(grossSalesAmount, refundAmount);

        // 수수료 계산 (순매출액 기준)
        BigDecimal commissionAmount = commissionCalculator.calculateCommission(netSalesAmount, commissionRate);

        // 부가세 계산 (수수료의 10%)
        BigDecimal taxAmount = commissionCalculator.calculateTax(commissionAmount);

        // 조정액 (현재는 0, 추후 수동 조정 기능 추가 시 사용)
        BigDecimal adjustmentAmount = BigDecimal.ZERO;

        // 최종 정산액 계산
        BigDecimal payoutAmount = commissionCalculator.calculatePayoutAmount(
            netSalesAmount, commissionAmount, taxAmount, adjustmentAmount
        );

        return new SettlementAmounts(
            commissionCalculator.normalize(grossSalesAmount),
            commissionCalculator.normalize(refundAmount),
            commissionRate,
            commissionAmount,
            taxAmount,
            adjustmentAmount,
            payoutAmount
        );
    }

    /**
     * Settlement Entity 생성
     */
    private Settlement createSettlement(Seller seller, SettlementAmounts amounts) {
        return Settlement.builder()
            .seller(seller)
            .cycleType(CycleType.DAILY)
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .grossSalesAmount(amounts.grossSalesAmount())
            .refundAmount(amounts.refundAmount())
            .commissionRate(amounts.commissionRate())
            .commissionAmount(amounts.commissionAmount())
            .taxAmount(amounts.taxAmount())
            .adjustmentAmount(amounts.adjustmentAmount())
            .payoutAmount(amounts.payoutAmount())
            .status(SettlementStatus.PENDING)
            .build();
    }

    /**
     * SettlementItem 목록 생성
     */
    private List<SettlementItem> createSettlementItems(List<Order> orders, List<Refund> refunds,
                                                        BigDecimal commissionRate) {
        List<SettlementItem> items = new ArrayList<>();

        // 판매 항목 생성
        for (Order order : orders) {
            for (OrderItem orderItem : order.getOrderItems()) {
                BigDecimal itemCommission = commissionCalculator.calculateCommission(
                    orderItem.getTotalAmount(), commissionRate
                );

                SettlementItem saleItem = SettlementItem.createSaleItem(
                    orderItem.getId(),
                    orderItem.getTotalAmount(),
                    commissionRate,
                    itemCommission
                );
                items.add(saleItem);
            }
        }

        // 환불 항목 생성
        for (Refund refund : refunds) {
            BigDecimal refundCommission = commissionCalculator.calculateCommission(
                refund.getRefundAmount(), commissionRate
            );

            SettlementItem refundItem = SettlementItem.createRefundItem(
                refund.getId(),
                refund.getRefundAmount(),
                commissionRate,
                refundCommission
            );
            items.add(refundItem);
        }

        return items;
    }

    /**
     * 정산 금액 계산 결과 Record
     */
    private record SettlementAmounts(
        BigDecimal grossSalesAmount,
        BigDecimal refundAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal taxAmount,
        BigDecimal adjustmentAmount,
        BigDecimal payoutAmount
    ) {}
}
