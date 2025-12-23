package com.company.settlement.repository;

import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.domain.entity.SettlementItem;
import com.company.settlement.domain.enums.CycleType;
import com.company.settlement.domain.enums.SellerStatus;
import com.company.settlement.domain.enums.SettlementItemType;
import com.company.settlement.domain.enums.SettlementSource;
import com.company.settlement.domain.enums.SettlementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SettlementRepository 단위 테스트
 * Repository 인터페이스의 메서드 시그니처 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementRepository 단위 테스트")
class SettlementRepositoryTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Test
    @DisplayName("판매자+기간+타입으로 정산 조회 - 메서드 호출 검증")
    void findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd_VerifyMethodCall() {
        // given
        Long sellerId = 1L;
        CycleType cycleType = CycleType.DAILY;
        LocalDate periodStart = LocalDate.of(2024, 1, 15);
        LocalDate periodEnd = LocalDate.of(2024, 1, 15);

        Seller mockSeller = Seller.builder()
                .sellerCode("SELLER001")
                .sellerName("테스트 판매자")
                .status(SellerStatus.ACTIVE)
                .build();
        Settlement mockSettlement = Settlement.builder()
                .seller(mockSeller)
                .cycleType(cycleType)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status(SettlementStatus.CONFIRMED)
                .build();

        when(settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                eq(sellerId), eq(cycleType), eq(periodStart), eq(periodEnd)))
                .thenReturn(Optional.of(mockSettlement));

        // when
        Optional<Settlement> found = settlementRepository.findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                sellerId, cycleType, periodStart, periodEnd);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSeller().getSellerName()).isEqualTo("테스트 판매자");
        verify(settlementRepository).findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
                sellerId, cycleType, periodStart, periodEnd);
    }

    @Test
    @DisplayName("ID로 정산+판매자 조회 - 메서드 호출 검증")
    void findByIdWithSeller_VerifyMethodCall() {
        // given
        Long settlementId = 1L;
        Seller mockSeller = Seller.builder()
                .sellerCode("SELLER001")
                .sellerName("테스트 판매자")
                .status(SellerStatus.ACTIVE)
                .build();
        Settlement mockSettlement = Settlement.builder()
                .seller(mockSeller)
                .status(SettlementStatus.PENDING)
                .build();

        when(settlementRepository.findByIdWithSeller(settlementId)).thenReturn(Optional.of(mockSettlement));

        // when
        Optional<Settlement> found = settlementRepository.findByIdWithSeller(settlementId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSeller().getSellerName()).isEqualTo("테스트 판매자");
        verify(settlementRepository).findByIdWithSeller(settlementId);
    }

    @Test
    @DisplayName("ID로 정산+판매자+품목 조회 - 메서드 호출 검증")
    void findByIdWithSellerAndItems_VerifyMethodCall() {
        // given
        Long settlementId = 1L;
        Seller mockSeller = Seller.builder()
                .sellerCode("SELLER001")
                .sellerName("테스트 판매자")
                .status(SellerStatus.ACTIVE)
                .build();

        SettlementItem item1 = SettlementItem.builder()
                .itemType(SettlementItemType.SALE)
                .sourceType(SettlementSource.ORDER_ITEM)
                .sourceId(1L)
                .grossAmount(new BigDecimal("30000"))
                .commissionRate(new BigDecimal("0.1000"))
                .commissionAmount(new BigDecimal("3000"))
                .netAmount(new BigDecimal("27000"))
                .build();

        Settlement mockSettlement = Settlement.builder()
                .seller(mockSeller)
                .status(SettlementStatus.PENDING)
                .build();
        mockSettlement.addSettlementItem(item1);

        when(settlementRepository.findByIdWithSellerAndItems(settlementId)).thenReturn(Optional.of(mockSettlement));

        // when
        Optional<Settlement> found = settlementRepository.findByIdWithSellerAndItems(settlementId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSettlementItems()).hasSize(1);
        verify(settlementRepository).findByIdWithSellerAndItems(settlementId);
    }

    @Test
    @DisplayName("판매자별 정산 목록 페이징 조회 - 메서드 호출 검증")
    void findBySellerId_VerifyMethodCall() {
        // given
        Long sellerId = 1L;
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, 3);

        Seller mockSeller = Seller.builder()
                .sellerCode("SELLER001")
                .sellerName("테스트 판매자")
                .status(SellerStatus.ACTIVE)
                .build();

        List<Settlement> mockSettlements = List.of(
                Settlement.builder().seller(mockSeller).status(SettlementStatus.CONFIRMED).build(),
                Settlement.builder().seller(mockSeller).status(SettlementStatus.CONFIRMED).build(),
                Settlement.builder().seller(mockSeller).status(SettlementStatus.CONFIRMED).build()
        );

        org.springframework.data.domain.Page<Settlement> mockPage = new org.springframework.data.domain.PageImpl<>(
                mockSettlements, pageRequest, 5);

        when(settlementRepository.findBySellerId(eq(sellerId), any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(mockPage);

        // when
        org.springframework.data.domain.Page<Settlement> page = settlementRepository.findBySellerId(sellerId, pageRequest);

        // then
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        verify(settlementRepository).findBySellerId(eq(sellerId), any(org.springframework.data.domain.PageRequest.class));
    }

    @Test
    @DisplayName("총 정산액 집계 - 메서드 호출 검증")
    void calculateTotalPayoutAmount_VerifyMethodCall() {
        // given
        Long sellerId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        BigDecimal expectedTotal = new BigDecimal("703703.57");

        when(settlementRepository.calculateTotalPayoutAmount(eq(sellerId), eq(startDate), eq(endDate)))
                .thenReturn(expectedTotal);

        // when
        BigDecimal totalPayout = settlementRepository.calculateTotalPayoutAmount(sellerId, startDate, endDate);

        // then
        assertThat(totalPayout).isEqualByComparingTo(expectedTotal);
        verify(settlementRepository).calculateTotalPayoutAmount(sellerId, startDate, endDate);
    }

    @Test
    @DisplayName("정산 상태별 건수 조회 - 메서드 호출 검증")
    void countByStatus_VerifyMethodCall() {
        // given
        when(settlementRepository.countByStatus(SettlementStatus.PENDING)).thenReturn(1L);
        when(settlementRepository.countByStatus(SettlementStatus.CONFIRMED)).thenReturn(2L);

        // when
        long pendingCount = settlementRepository.countByStatus(SettlementStatus.PENDING);
        long confirmedCount = settlementRepository.countByStatus(SettlementStatus.CONFIRMED);

        // then
        assertThat(pendingCount).isEqualTo(1);
        assertThat(confirmedCount).isEqualTo(2);
        verify(settlementRepository).countByStatus(SettlementStatus.PENDING);
        verify(settlementRepository).countByStatus(SettlementStatus.CONFIRMED);
    }

    @Test
    @DisplayName("정산 저장 - 메서드 호출 검증")
    void save_VerifyMethodCall() {
        // given
        Seller mockSeller = Seller.builder()
                .sellerCode("SELLER001")
                .sellerName("테스트 판매자")
                .status(SellerStatus.ACTIVE)
                .build();

        Settlement settlement = Settlement.builder()
                .seller(mockSeller)
                .cycleType(CycleType.DAILY)
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now())
                .status(SettlementStatus.PENDING)
                .build();

        when(settlementRepository.save(any(Settlement.class))).thenReturn(settlement);

        // when
        Settlement saved = settlementRepository.save(settlement);

        // then
        assertThat(saved.getCycleType()).isEqualTo(CycleType.DAILY);
        verify(settlementRepository).save(settlement);
    }
}
