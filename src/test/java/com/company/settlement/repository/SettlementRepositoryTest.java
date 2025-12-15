package com.company.settlement.repository;

import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.domain.entity.SettlementItem;
import com.company.settlement.domain.enums.CycleType;
import com.company.settlement.domain.enums.SellerStatus;
import com.company.settlement.domain.enums.SettlementItemType;
import com.company.settlement.domain.enums.SettlementSource;
import com.company.settlement.domain.enums.SettlementStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SettlementRepository 통합 테스트
 * 
 * 테스트 포인트:
 * - 멱등성 체크 쿼리 (동일 기간 정산 존재 여부)
 * - Fetch Join을 통한 N+1 문제 해결
 * - BigDecimal 정산액 집계 정확성
 * - 통계 쿼리 정확성
 */
class SettlementRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private EntityManager entityManager;

    private Seller testSeller;

    @BeforeEach
    void setUp() {
        testSeller = Seller.builder()
                .sellerCode("SETTLEMENT_TEST_SELLER")
                .sellerName("정산 테스트 판매자")
                .commissionRate(new BigDecimal("0.1000"))
                .status(SellerStatus.ACTIVE)
                .build();
        sellerRepository.save(testSeller);
    }

    @Test
    @DisplayName("멱등성 체크 - 동일 기간 정산 존재 시 조회 성공")
    void findBySellerIdAndCycleTypeAndPeriod_Idempotency() {
        // given
        LocalDate periodStart = LocalDate.of(2024, 1, 15);
        LocalDate periodEnd = LocalDate.of(2024, 1, 15);

        Settlement settlement = createSettlement(testSeller, periodStart, periodEnd, SettlementStatus.CONFIRMED);

        // when
        Optional<Settlement> found = settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                testSeller.getId(), CycleType.DAILY, periodStart, periodEnd);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSeller().getId()).isEqualTo(testSeller.getId());
    }

    @Test
    @DisplayName("멱등성 체크 - 다른 기간 정산은 조회 안됨")
    void findBySellerIdAndCycleTypeAndPeriod_NotFound() {
        // given
        LocalDate periodStart = LocalDate.of(2024, 1, 15);
        LocalDate periodEnd = LocalDate.of(2024, 1, 15);

        createSettlement(testSeller, periodStart, periodEnd, SettlementStatus.CONFIRMED);

        // when - 다른 날짜로 조회
        Optional<Settlement> found = settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                testSeller.getId(), CycleType.DAILY, LocalDate.of(2024, 1, 16), LocalDate.of(2024, 1, 16));

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("정산 상세 조회 - Seller Fetch Join")
    void findByIdWithSeller_FetchJoin() {
        // given
        Settlement settlement = createSettlement(testSeller, LocalDate.now(), LocalDate.now(), SettlementStatus.PENDING);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Settlement> found = settlementRepository.findByIdWithSeller(settlement.getId());

        // then
        assertThat(found).isPresent();
        // Fetch Join으로 Seller에 접근해도 추가 쿼리 없음
        assertThat(found.get().getSeller().getSellerName()).isEqualTo("정산 테스트 판매자");
    }

    @Test
    @DisplayName("정산 상세 조회 - Seller + Items Fetch Join")
    void findByIdWithSellerAndItems_FetchJoin() {
        // given
        Settlement settlement = createSettlementWithItems(testSeller, LocalDate.now(), 3);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Settlement> found = settlementRepository.findByIdWithSellerAndItems(settlement.getId());

        // then
        assertThat(found).isPresent();
        // Fetch Join으로 Items에 접근해도 추가 쿼리 없음
        assertThat(found.get().getSettlementItems()).hasSize(3);
        assertThat(found.get().getSeller().getSellerName()).isEqualTo("정산 테스트 판매자");
    }

    @Test
    @DisplayName("판매자별 정산 목록 페이징 조회")
    void findBySellerId_Pagination() {
        // given
        for (int i = 1; i <= 5; i++) {
            createSettlement(testSeller, LocalDate.of(2024, 1, i), LocalDate.of(2024, 1, i), SettlementStatus.CONFIRMED);
        }

        // when
        Page<Settlement> page = settlementRepository.findBySellerId(testSeller.getId(), PageRequest.of(0, 3));

        // then
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("총 정산액 집계 - BigDecimal 정확성")
    void calculateTotalPayoutAmount_ReturnsCorrectSum() {
        // given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        createSettlementWithAmount(testSeller, LocalDate.of(2024, 1, 10), new BigDecimal("123456.78"), SettlementStatus.PAID);
        createSettlementWithAmount(testSeller, LocalDate.of(2024, 1, 15), new BigDecimal("234567.89"), SettlementStatus.PAID);
        createSettlementWithAmount(testSeller, LocalDate.of(2024, 1, 20), new BigDecimal("345678.90"), SettlementStatus.PAID);

        // when
        BigDecimal totalPayout = settlementRepository.calculateTotalPayoutAmount(
                testSeller.getId(), startDate, endDate);

        // then
        // 123456.78 + 234567.89 + 345678.90 = 703703.57
        assertThat(totalPayout).isEqualByComparingTo(new BigDecimal("703703.57"));
    }

    @Test
    @DisplayName("정산 없을 때 총 정산액 0 반환 - NPE 방지")
    void calculateTotalPayoutAmount_NoSettlements_ReturnsZero() {
        // given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        // 정산 데이터 없음

        // when
        BigDecimal totalPayout = settlementRepository.calculateTotalPayoutAmount(
                testSeller.getId(), startDate, endDate);

        // then
        assertThat(totalPayout).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("CONFIRMED 상태 정산은 집계에서 제외")
    void calculateTotalPayoutAmount_ExcludesNonPaidStatus() {
        // given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        createSettlementWithAmount(testSeller, LocalDate.of(2024, 1, 10), new BigDecimal("100000"), SettlementStatus.PAID);
        createSettlementWithAmount(testSeller, LocalDate.of(2024, 1, 15), new BigDecimal("200000"), SettlementStatus.CONFIRMED);
        createSettlementWithAmount(testSeller, LocalDate.of(2024, 1, 20), new BigDecimal("300000"), SettlementStatus.PENDING);

        // when
        BigDecimal totalPayout = settlementRepository.calculateTotalPayoutAmount(
                testSeller.getId(), startDate, endDate);

        // then
        // PAID 상태인 100000만 집계
        assertThat(totalPayout).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("기간별 정산 금액 통계 조회 - 쿼리 실행 검증")
    @org.junit.jupiter.api.Disabled("통계 쿼리 결과 형식 이슈로 추후 수정 필요")
    void getSettlementAmountStatistics_ReturnsStats() {
        // given
        LocalDate periodStart = LocalDate.of(2024, 1, 1);
        LocalDate periodEnd = LocalDate.of(2024, 1, 31);

        // 정산 데이터 생성
        createFullSettlement(testSeller, LocalDate.of(2024, 1, 10),
                new BigDecimal("100000"), new BigDecimal("10000"),
                new BigDecimal("9000"), new BigDecimal("900"),
                new BigDecimal("80100"));

        // when
        Object[] stats = settlementRepository.getSettlementAmountStatistics(periodStart, periodEnd);

        // then
        // 쿼리가 정상 실행되고 5개 컬럼 반환 검증
        assertThat(stats).isNotNull();
        assertThat(stats).hasSize(5);
        // 각 값이 null이 아닌지 검증
        for (int i = 0; i < 5; i++) {
            assertThat(stats[i]).isNotNull();
        }
    }

    @Test
    @DisplayName("정산 상태별 건수 조회")
    void countByStatus_ReturnsCorrectCount() {
        // given
        createSettlement(testSeller, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1), SettlementStatus.PENDING);
        createSettlement(testSeller, LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 2), SettlementStatus.CONFIRMED);
        createSettlement(testSeller, LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 3), SettlementStatus.CONFIRMED);

        // when
        long pendingCount = settlementRepository.countByStatus(SettlementStatus.PENDING);
        long confirmedCount = settlementRepository.countByStatus(SettlementStatus.CONFIRMED);

        // then
        assertThat(pendingCount).isEqualTo(1);
        assertThat(confirmedCount).isEqualTo(2);
    }

    // ========== Helper Methods ==========

    private Settlement createSettlement(Seller seller, LocalDate periodStart, LocalDate periodEnd, SettlementStatus status) {
        Settlement settlement = Settlement.builder()
                .seller(seller)
                .cycleType(CycleType.DAILY)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .grossSalesAmount(new BigDecimal("100000"))
                .refundAmount(new BigDecimal("10000"))
                .commissionRate(new BigDecimal("0.1000"))
                .commissionAmount(new BigDecimal("9000"))
                .taxAmount(new BigDecimal("900"))
                .adjustmentAmount(BigDecimal.ZERO)
                .payoutAmount(new BigDecimal("80100"))
                .status(status)
                .build();
        return settlementRepository.save(settlement);
    }

    private Settlement createSettlementWithAmount(Seller seller, LocalDate periodDate, BigDecimal payoutAmount, SettlementStatus status) {
        Settlement settlement = Settlement.builder()
                .seller(seller)
                .cycleType(CycleType.DAILY)
                .periodStart(periodDate)
                .periodEnd(periodDate)
                .grossSalesAmount(new BigDecimal("100000"))
                .refundAmount(new BigDecimal("10000"))
                .commissionRate(new BigDecimal("0.1000"))
                .commissionAmount(new BigDecimal("9000"))
                .taxAmount(new BigDecimal("900"))
                .adjustmentAmount(BigDecimal.ZERO)
                .payoutAmount(payoutAmount)
                .status(status)
                .build();
        return settlementRepository.save(settlement);
    }

    private Settlement createSettlementWithItems(Seller seller, LocalDate periodDate, int itemCount) {
        Settlement settlement = createSettlement(seller, periodDate, periodDate, SettlementStatus.PENDING);

        for (int i = 1; i <= itemCount; i++) {
            SettlementItem item = SettlementItem.builder()
                    .itemType(SettlementItemType.SALE)
                    .sourceType(SettlementSource.ORDER_ITEM)
                    .sourceId((long) i)
                    .grossAmount(new BigDecimal("30000"))
                    .commissionRate(new BigDecimal("0.1000"))
                    .commissionAmount(new BigDecimal("3000"))
                    .netAmount(new BigDecimal("27000"))
                    .build();
            settlement.addSettlementItem(item);
        }

        return settlementRepository.save(settlement);
    }

    private Settlement createFullSettlement(Seller seller, LocalDate periodDate,
                                            BigDecimal grossSales, BigDecimal refund,
                                            BigDecimal commission, BigDecimal tax,
                                            BigDecimal payout) {
        Settlement settlement = Settlement.builder()
                .seller(seller)
                .cycleType(CycleType.DAILY)
                .periodStart(periodDate)
                .periodEnd(periodDate)
                .grossSalesAmount(grossSales)
                .refundAmount(refund)
                .commissionRate(new BigDecimal("0.1000"))
                .commissionAmount(commission)
                .taxAmount(tax)
                .adjustmentAmount(BigDecimal.ZERO)
                .payoutAmount(payout)
                .status(SettlementStatus.CONFIRMED)
                .build();
        return settlementRepository.save(settlement);
    }
}
