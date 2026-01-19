package com.company.settlement.batch.listener;

import com.company.settlement.batch.dto.SettlementContext;
import com.company.settlement.batch.exception.SettlementAlreadyExistsException;
import com.company.settlement.batch.exception.SettlementProcessingException;
import com.company.settlement.batch.support.SettlementTestDataFactory;
import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.entity.Settlement;

import java.util.List;
import com.company.settlement.domain.entity.SettlementJobExecution;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * SettlementItemSkipListener 단위 테스트
 *
 * 검증 항목:
 * - onSkipInProcess: 멱등성 체크로 인한 Skip 로깅
 * - onSkipInProcess: 처리 오류로 인한 Skip 로깅
 * - onSkipInWrite: 저장 중 오류 로깅
 * - onSkipInRead: 읽기 중 오류 로깅
 * - skippedSellers 리스트 관리
 * - JobExecution 통계 업데이트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementItemSkipListener 단위 테스트")
class SettlementItemSkipListenerTest {

    @Mock
    private DailySettlementJobListener dailySettlementJobListener;

    @InjectMocks
    private SettlementItemSkipListener listener;

    private Seller testSeller;
    private SettlementContext testContext;

    @BeforeEach
    void setUp() {
        listener.clear(); // 각 테스트 전에 상태 초기화

        testSeller = SettlementTestDataFactory.aSeller();

        LocalDate targetDate = LocalDate.of(2025, 1, 15);
        Settlement settlement = SettlementTestDataFactory.aDailySettlement(testSeller, targetDate);
        testContext = new SettlementContext(settlement, List.of());
    }

    @Nested
    @DisplayName("onSkipInProcess: Process 단계 Skip 처리")
    class OnSkipInProcess {

        @Test
        @DisplayName("이미 정산이 존재하는 경우 ALREADY_EXISTS 사유로 기록해야 한다")
        void recordAlreadyExistsReason() {
            // Given
            SettlementAlreadyExistsException exception = new SettlementAlreadyExistsException(
                "Settlement already exists: sellerId=1"
            );
            SettlementJobExecution jobExecution = mock(SettlementJobExecution.class);
            // ALREADY_EXISTS인 경우 getCurrentExecution()이 호출되지 않으므로 lenient() 사용
            lenient().when(dailySettlementJobListener.getCurrentExecution()).thenReturn(jobExecution);

            // When
            listener.onSkipInProcess(testSeller, exception);

            // Then
            assertThat(listener.getSkippedSellers()).hasSize(1);

            SettlementItemSkipListener.SkippedSellerInfo skippedInfo =
                listener.getSkippedSellers().get(0);
            assertThat(skippedInfo.sellerId()).isEqualTo(testSeller.getId());
            assertThat(skippedInfo.sellerCode()).isEqualTo(testSeller.getSellerCode());
            assertThat(skippedInfo.sellerName()).isEqualTo(testSeller.getSellerName());
            assertThat(skippedInfo.reason()).isEqualTo(
                SettlementItemSkipListener.SkipReason.ALREADY_EXISTS
            );
            assertThat(skippedInfo.errorMessage()).contains("Settlement already exists");
            assertThat(skippedInfo.skippedAt()).isNotNull();

            // ALREADY_EXISTS는 실패 카운트가 아님 (getCurrentExecution이 호출되지 않음)
            verify(jobExecution, never()).incrementFailureCount(any());
        }

        @Test
        @DisplayName("처리 오류인 경우 PROCESSING_ERROR 사유로 기록해야 한다")
        void recordProcessingErrorReason() {
            // Given
            SettlementProcessingException exception = new SettlementProcessingException(
                "Failed to calculate settlement"
            );
            SettlementJobExecution jobExecution = mock(SettlementJobExecution.class);
            when(dailySettlementJobListener.getCurrentExecution()).thenReturn(jobExecution);

            // When
            listener.onSkipInProcess(testSeller, exception);

            // Then
            assertThat(listener.getSkippedSellers()).hasSize(1);

            SettlementItemSkipListener.SkippedSellerInfo skippedInfo =
                listener.getSkippedSellers().get(0);
            assertThat(skippedInfo.reason()).isEqualTo(
                SettlementItemSkipListener.SkipReason.PROCESSING_ERROR
            );
            assertThat(skippedInfo.errorMessage()).contains("Failed to calculate settlement");

            // PROCESSING_ERROR는 실패 카운트 증가
            verify(jobExecution).incrementFailureCount("Failed to calculate settlement");
        }

        @Test
        @DisplayName("알 수 없는 예외인 경우 UNKNOWN 사유로 기록해야 한다")
        void recordUnknownErrorReason() {
            // Given
            RuntimeException exception = new RuntimeException("Unexpected error");
            SettlementJobExecution jobExecution = mock(SettlementJobExecution.class);
            when(dailySettlementJobListener.getCurrentExecution()).thenReturn(jobExecution);

            // When
            listener.onSkipInProcess(testSeller, exception);

            // Then
            assertThat(listener.getSkippedSellers()).hasSize(1);

            SettlementItemSkipListener.SkippedSellerInfo skippedInfo =
                listener.getSkippedSellers().get(0);
            assertThat(skippedInfo.reason()).isEqualTo(
                SettlementItemSkipListener.SkipReason.UNKNOWN
            );
            assertThat(skippedInfo.errorMessage()).contains("Unexpected error");

            verify(jobExecution).incrementFailureCount("Unexpected error");
        }

        @Test
        @DisplayName("여러 Skip을 순차적으로 기록해야 한다")
        void recordMultipleSkips() {
            // Given
            Seller seller2 = SettlementTestDataFactory.aSellerWith5Percent();
            SettlementJobExecution jobExecution = mock(SettlementJobExecution.class);
            when(dailySettlementJobListener.getCurrentExecution()).thenReturn(jobExecution);

            // When
            listener.onSkipInProcess(testSeller, new SettlementProcessingException("Error 1"));
            listener.onSkipInProcess(seller2, new SettlementProcessingException("Error 2"));

            // Then
            assertThat(listener.getSkippedSellers()).hasSize(2);
            assertThat(listener.getSkippedSellers().get(0).sellerCode()).isEqualTo("SELLER001");
            assertThat(listener.getSkippedSellers().get(1).sellerCode()).isEqualTo("SELLER002");
        }
    }

    @Nested
    @DisplayName("onSkipInWrite: Write 단계 Skip 처리")
    class OnSkipInWrite {

        @Test
        @DisplayName("Write 중 오류가 발생하면 WRITE_ERROR 사유로 기록해야 한다")
        void recordWriteErrorReason() {
            // Given
            RuntimeException exception = new RuntimeException("Database constraint violation");
            SettlementJobExecution jobExecution = mock(SettlementJobExecution.class);
            when(dailySettlementJobListener.getCurrentExecution()).thenReturn(jobExecution);

            // When
            listener.onSkipInWrite(testContext, exception);

            // Then
            assertThat(listener.getSkippedSellers()).hasSize(1);

            SettlementItemSkipListener.SkippedSellerInfo skippedInfo =
                listener.getSkippedSellers().get(0);
            assertThat(skippedInfo.reason()).isEqualTo(
                SettlementItemSkipListener.SkipReason.WRITE_ERROR
            );
            assertThat(skippedInfo.errorMessage()).contains("Database constraint violation");

            verify(jobExecution).incrementFailureCount("Database constraint violation");
        }
    }

    @Nested
    @DisplayName("onSkipInRead: Read 단계 Skip 처리")
    class OnSkipInRead {

        @Test
        @DisplayName("Read 중 오류가 발생하면 로그만 출력해야 한다")
        void logReadError() {
            // Given
            RuntimeException exception = new RuntimeException("Connection timeout");

            // When
            listener.onSkipInRead(exception);

            // Then
            // skippedSellers 리스트에는 추가되지 않음 (Read 단계는 Seller 정보가 없음)
            assertThat(listener.getSkippedSellers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getErrorSkipCount: 에러 Skip 수 계산")
    class GetErrorSkipCount {

        @Test
        @DisplayName("ALREADY_EXISTS를 제외한 Skip 수를 반환해야 한다")
        void countSkipsExcludingAlreadyExists() {
            // Given
            Seller seller1 = SettlementTestDataFactory.aSeller();
            Seller seller2 = SettlementTestDataFactory.aSellerWith5Percent();
            Seller seller3 = SettlementTestDataFactory.aSellerWith15Percent();
            SettlementJobExecution jobExecution = mock(SettlementJobExecution.class);
            when(dailySettlementJobListener.getCurrentExecution()).thenReturn(jobExecution);

            listener.onSkipInProcess(seller1, new SettlementAlreadyExistsException("Exists"));
            listener.onSkipInProcess(seller2, new SettlementProcessingException("Error"));
            listener.onSkipInProcess(seller3, new RuntimeException("Unknown"));

            // When
            long errorSkipCount = listener.getErrorSkipCount();

            // Then
            // ALREADY_EXISTS 1개 제외, PROCESSING_ERROR + UNKNOWN = 2개
            assertThat(errorSkipCount).isEqualTo(2);
        }

        @Test
        @DisplayName("모든 Skip이 ALREADY_EXISTS이면 0을 반환해야 한다")
        void returnZeroWhenAllAreAlreadyExists() {
            // Given
            listener.onSkipInProcess(testSeller, new SettlementAlreadyExistsException("Exists"));

            // When
            long errorSkipCount = listener.getErrorSkipCount();

            // Then
            assertThat(errorSkipCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("clear: 상태 초기화")
    class Clear {

        @Test
        @DisplayName("clear 호출 시 skippedSellers 리스트가 비어야 한다")
        void clearSkippedSellers() {
            // Given
            SettlementJobExecution jobExecution = mock(SettlementJobExecution.class);
            when(dailySettlementJobListener.getCurrentExecution()).thenReturn(jobExecution);

            listener.onSkipInProcess(testSeller, new SettlementProcessingException("Error"));
            assertThat(listener.getSkippedSellers()).isNotEmpty();

            // When
            listener.clear();

            // Then
            assertThat(listener.getSkippedSellers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("JobExecution이 null인 경우")
    class NullJobExecution {

        @Test
        @DisplayName("JobExecution이 null이면 실패 카운트를 증가시키지 않아야 한다")
        void doNotIncrementWhenJobExecutionIsNull() {
            // Given - getCurrentExecution이 null을 반환하도록 설정
            when(dailySettlementJobListener.getCurrentExecution()).thenReturn(null);

            // When
            listener.onSkipInProcess(testSeller, new SettlementProcessingException("Error"));

            // Then
            assertThat(listener.getSkippedSellers()).hasSize(1);
            // 예외 발생 없이 정상 처리
        }
    }
}
