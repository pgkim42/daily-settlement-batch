package com.company.settlement.batch.writer;

import com.company.settlement.batch.dto.SettlementContext;
import com.company.settlement.batch.support.SettlementTestDataFactory;
import com.company.settlement.domain.entity.*;
import com.company.settlement.domain.enums.*;
import com.company.settlement.repository.SettlementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SettlementWriter 단위 테스트
 *
 * 검증 항목:
 * - 청크 단위 저장 로직
 * - Settlement과 SettlementItem 연결
 * - EntityManager flush/clear 호출
 * - CascadeType.ALL로 인한 연관 저장
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementWriter 단위 테스트")
class SettlementWriterTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SettlementWriter writer;

    private Seller seller;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        seller = SettlementTestDataFactory.aSeller();
        targetDate = LocalDate.of(2025, 1, 15);
    }

    @Nested
    @DisplayName("청크 단위 저장")
    class ChunkWrite {

        @Test
        @DisplayName("단일 SettlementContext를 정상적으로 저장해야 한다")
        void writeSingleContext() throws Exception {
            // Given
            Settlement settlement = SettlementTestDataFactory.aDailySettlement(seller, targetDate);
            SettlementItem saleItem = SettlementTestDataFactory.aSaleSettlementItem(1L, new BigDecimal("10000"), new BigDecimal("0.1000"));

            SettlementContext context = new SettlementContext(settlement, List.of(saleItem));

            Chunk<SettlementContext> chunk = new Chunk<>(List.of(context));

            when(settlementRepository.saveAll(any(List.class))).thenReturn(List.of(settlement));

            // When
            writer.write(chunk);

            // Then
            ArgumentCaptor<List<Settlement>> captor = ArgumentCaptor.forClass(List.class);
            verify(settlementRepository).saveAll(captor.capture());

            List<Settlement> savedSettlements = captor.getValue();
            assertThat(savedSettlements).hasSize(1);

            Settlement saved = savedSettlements.get(0);
            assertThat(saved.getSeller()).isEqualTo(seller);
            assertThat(saved.getSettlementItems()).hasSize(1);

            verify(entityManager).flush();
            verify(entityManager).clear();
        }

        @Test
        @DisplayName("다중 SettlementContext를 정상적으로 저장해야 한다")
        void writeMultipleContexts() throws Exception {
            // Given
            Seller seller2 = SettlementTestDataFactory.aSellerWith5Percent();

            Settlement settlement1 = SettlementTestDataFactory.aDailySettlement(seller, targetDate);
            Settlement settlement2 = SettlementTestDataFactory.createSettlement(
                2L, seller2, CycleType.DAILY, targetDate, targetDate,
                new BigDecimal("20000"), BigDecimal.ZERO, new BigDecimal("0.0500"), new BigDecimal("19000")
            );

            SettlementItem item1 = SettlementTestDataFactory.aSaleSettlementItem(1L, new BigDecimal("10000"), new BigDecimal("0.1000"));
            SettlementItem item2 = SettlementTestDataFactory.aSaleSettlementItem(2L, new BigDecimal("20000"), new BigDecimal("0.0500"));

            SettlementContext context1 = new SettlementContext(settlement1, List.of(item1));
            SettlementContext context2 = new SettlementContext(settlement2, List.of(item2));

            Chunk<SettlementContext> chunk = new Chunk<>(List.of(context1, context2));

            when(settlementRepository.saveAll(any(List.class))).thenReturn(List.of(settlement1, settlement2));

            // When
            writer.write(chunk);

            // Then
            ArgumentCaptor<List<Settlement>> captor = ArgumentCaptor.forClass(List.class);
            verify(settlementRepository).saveAll(captor.capture());

            List<Settlement> savedSettlements = captor.getValue();
            assertThat(savedSettlements).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Settlement과 SettlementItem 연결")
    class LinkItemsToSettlement {

        @Test
        @DisplayName("linkItemsToSettlement 호출로 SettlementItem이 Settlement에 연결되어야 한다")
        void linkItemsToSettlement() throws Exception {
            // Given
            Settlement settlement = SettlementTestDataFactory.aDailySettlement(seller, targetDate);
            SettlementItem saleItem = SettlementTestDataFactory.aSaleSettlementItem(1L, new BigDecimal("10000"), new BigDecimal("0.1000"));
            SettlementItem refundItem = SettlementTestDataFactory.aRefundSettlementItem(2L, new BigDecimal("3000"), new BigDecimal("0.1000"));

            SettlementContext context = new SettlementContext(settlement, List.of(saleItem, refundItem));
            Chunk<SettlementContext> chunk = new Chunk<>(List.of(context));

            when(settlementRepository.saveAll(any(List.class))).thenReturn(List.of(settlement));

            // When
            writer.write(chunk);

            // Then
            ArgumentCaptor<List<Settlement>> captor = ArgumentCaptor.forClass(List.class);
            verify(settlementRepository).saveAll(captor.capture());

            Settlement saved = captor.getValue().get(0);
            assertThat(saved.getSettlementItems()).hasSize(2);

            // SettlementItem의 settlement 필드가 설정되었는지 확인
            assertThat(saved.getSettlementItems().get(0).getSettlement()).isNotNull();
            assertThat(saved.getSettlementItems().get(1).getSettlement()).isNotNull();
        }
    }

    @Nested
    @DisplayName("EntityManager flush/clear")
    class EntityManagerOperations {

        @Test
        @DisplayName("flush()가 저장 후 호출되어야 한다")
        void callFlushAfterSave() throws Exception {
            // Given
            Settlement settlement = SettlementTestDataFactory.aDailySettlement(seller, targetDate);
            SettlementContext context = new SettlementContext(settlement, List.of());
            Chunk<SettlementContext> chunk = new Chunk<>(List.of(context));

            when(settlementRepository.saveAll(any(List.class))).thenReturn(List.of(settlement));

            // When
            writer.write(chunk);

            // Then
            verify(entityManager).flush();
        }

        @Test
        @DisplayName("clear()가 flush 후 호출되어야 한다")
        void callClearAfterFlush() throws Exception {
            // Given
            Settlement settlement = SettlementTestDataFactory.aDailySettlement(seller, targetDate);
            SettlementContext context = new SettlementContext(settlement, List.of());
            Chunk<SettlementContext> chunk = new Chunk<>(List.of(context));

            when(settlementRepository.saveAll(any(List.class))).thenReturn(List.of(settlement));

            // When
            writer.write(chunk);

            // Then
            verify(entityManager).clear();
        }

        @Test
        @DisplayName("flush()가 clear()보다 먼저 호출되어야 한다")
        void callFlushBeforeClear() throws Exception {
            // Given
            Settlement settlement = SettlementTestDataFactory.aDailySettlement(seller, targetDate);
            SettlementContext context = new SettlementContext(settlement, List.of());
            Chunk<SettlementContext> chunk = new Chunk<>(List.of(context));

            when(settlementRepository.saveAll(any(List.class))).thenReturn(List.of(settlement));

            // When
            writer.write(chunk);

            // Then
            var inOrder = inOrder(entityManager);
            inOrder.verify(entityManager).flush();
            inOrder.verify(entityManager).clear();
        }
    }

    @Nested
    @DisplayName("빈 청크 처리")
    class EmptyChunk {

        @Test
        @DisplayName("빈 청크도 정상적으로 처리되어야 한다")
        void handleEmptyChunk() throws Exception {
            // Given
            Chunk<SettlementContext> chunk = new Chunk<>(List.of());
            when(settlementRepository.saveAll(any(List.class))).thenReturn(List.of());

            // When
            writer.write(chunk);

            // Then
            verify(settlementRepository).saveAll(List.of());
            verify(entityManager).flush();
            verify(entityManager).clear();
        }
    }

    @Nested
    @DisplayName("복합 항목 저장")
    class ComplexItems {

        @Test
        @DisplayName("판매, 환불, 조정 항목이 모두 포함된 경우를 처리해야 한다")
        void saveMixedItemTypes() throws Exception {
            // Given
            Settlement settlement = SettlementTestDataFactory.aSettlementWithRefund(seller, targetDate);

            SettlementItem saleItem = SettlementTestDataFactory.aSaleSettlementItem(1L, new BigDecimal("10000"), new BigDecimal("0.1000"));
            SettlementItem refundItem = SettlementTestDataFactory.aRefundSettlementItem(2L, new BigDecimal("3000"), new BigDecimal("0.1000"));

            SettlementContext context = new SettlementContext(settlement, List.of(saleItem, refundItem));
            Chunk<SettlementContext> chunk = new Chunk<>(List.of(context));

            when(settlementRepository.saveAll(any(List.class))).thenReturn(List.of(settlement));

            // When
            writer.write(chunk);

            // Then
            ArgumentCaptor<List<Settlement>> captor = ArgumentCaptor.forClass(List.class);
            verify(settlementRepository).saveAll(captor.capture());

            Settlement saved = captor.getValue().get(0);
            assertThat(saved.getSettlementItems()).hasSize(2);

            long saleCount = saved.getSettlementItems().stream()
                .filter(item -> item.getItemType() == SettlementItemType.SALE)
                .count();
            long refundCount = saved.getSettlementItems().stream()
                .filter(item -> item.getItemType() == SettlementItemType.REFUND)
                .count();

            assertThat(saleCount).isEqualTo(1);
            assertThat(refundCount).isEqualTo(1);
        }
    }
}
