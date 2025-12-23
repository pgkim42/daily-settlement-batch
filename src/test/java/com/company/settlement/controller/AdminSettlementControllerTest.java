package com.company.settlement.controller;

import com.company.settlement.dto.request.BatchTriggerRequest;
import com.company.settlement.dto.response.BatchTriggerResponse;
import com.company.settlement.dto.response.SettlementDetailResponse;
import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.dto.response.SettlementStatisticsResponse;
import com.company.settlement.exception.BatchAlreadyRunningException;
import com.company.settlement.exception.SettlementNotFoundException;
import com.company.settlement.service.AdminSettlementService;
import com.company.settlement.service.BatchTriggerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdminSettlementController 단위 테스트
 * - 관리자용 정산 API 엔드포인트 테스트
 * - 서비스 계층 목킹으로 Controller 로직만 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSettlementController 단위 테스트")
class AdminSettlementControllerTest {

    @Mock
    private AdminSettlementService adminSettlementService;

    @Mock
    private BatchTriggerService batchTriggerService;

    @InjectMocks
    private AdminSettlementController adminSettlementController;

    @Nested
    @DisplayName("GET /api/admin/settlements - 전체 정산 목록 조회")
    class GetAllSettlements {

        @Test
        @DisplayName("성공: 전체 정산 목록 조회")
        void getAllSettlements_Success() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            Page<SettlementResponse> mockResponse = createMockSettlementResponsePage();

            when(adminSettlementService.getAllSettlements(any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when
            ResponseEntity<Page<SettlementResponse>> response = adminSettlementController.getAllSettlements(pageRequest);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).sellerCode()).isEqualTo("SELLER001");
        }

        @Test
        @DisplayName("성공: 빈 정산 목록 조회")
        void getAllSettlements_EmptyResult() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<SettlementResponse> emptyResponse = Page.empty();

            when(adminSettlementService.getAllSettlements(any(PageRequest.class)))
                .thenReturn(emptyResponse);

            // when
            ResponseEntity<Page<SettlementResponse>> response = adminSettlementController.getAllSettlements(pageRequest);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/admin/settlements/period - 기간별 정산 목록 조회")
    class GetSettlementsByPeriod {

        @Test
        @DisplayName("성공: 유효한 기간으로 정산 목록 조회")
        void getSettlementsByPeriod_Success() {
            // given
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<SettlementResponse> mockResponse = createMockSettlementResponsePage();

            when(adminSettlementService.getSettlementsByPeriod(eq(startDate), eq(endDate), any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when
            ResponseEntity<Page<SettlementResponse>> response = adminSettlementController.getSettlementsByPeriod(startDate, endDate, pageRequest);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/settlements/{settlementId} - 관리자용 정산 상세 조회")
    class GetSettlementDetail {

        @Test
        @DisplayName("성공: 정산 상세 조회")
        void getSettlementDetail_Success() {
            // given
            Long settlementId = 1L;
            SettlementDetailResponse mockResponse = createMockSettlementDetailResponse();

            when(adminSettlementService.getSettlementDetail(eq(settlementId)))
                .thenReturn(mockResponse);

            // when
            ResponseEntity<SettlementDetailResponse> response = adminSettlementController.getSettlementDetail(settlementId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().id()).isEqualTo(settlementId);
            assertThat(response.getBody().seller().sellerCode()).isEqualTo("SELLER001");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 정산 ID")
        void getSettlementDetail_NotFound() {
            // given
            Long nonExistentId = 999L;
            String errorMessage = "Settlement not found: " + nonExistentId;

            when(adminSettlementService.getSettlementDetail(eq(nonExistentId)))
                .thenThrow(new SettlementNotFoundException(errorMessage));

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> adminSettlementController.getSettlementDetail(nonExistentId))
                .isInstanceOf(SettlementNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/settlements/statistics - 정산 통계 조회")
    class GetSettlementStatistics {

        @Test
        @DisplayName("성공: 정산 통계 조회")
        void getSettlementStatistics_Success() {
            // given
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);
            SettlementStatisticsResponse mockResponse = createMockStatisticsResponse();

            when(adminSettlementService.getStatistics(eq(startDate), eq(endDate)))
                .thenReturn(mockResponse);

            // when
            ResponseEntity<SettlementStatisticsResponse> response = adminSettlementController.getStatistics(startDate, endDate);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().totalSettlementCount()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("POST /api/admin/settlements/batch/trigger - 배치 수동 실행")
    class TriggerBatch {

        @Test
        @DisplayName("성공: 유효한 날짜로 배치 실행")
        void triggerBatch_Success() {
            // given
            LocalDate targetDate = LocalDate.now().minusDays(1);
            BatchTriggerRequest request = new BatchTriggerRequest(targetDate);
            BatchTriggerResponse mockResponse = createMockBatchTriggerResponse();

            when(batchTriggerService.triggerSettlementBatch(eq(targetDate)))
                .thenReturn(mockResponse);

            // when
            ResponseEntity<BatchTriggerResponse> response = adminSettlementController.triggerBatch(request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().jobExecutionId()).isEqualTo(123);
            assertThat(response.getBody().jobName()).isEqualTo("dailySettlementJob");
        }

        @Test
        @DisplayName("실패: 이미 실행 중인 배치")
        void triggerBatch_BatchAlreadyRunning() {
            // given
            LocalDate targetDate = LocalDate.now().minusDays(1);
            BatchTriggerRequest request = new BatchTriggerRequest(targetDate);
            String errorMessage = "Batch job is already running: dailySettlementJob";

            when(batchTriggerService.triggerSettlementBatch(eq(targetDate)))
                .thenThrow(new BatchAlreadyRunningException(errorMessage));

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> adminSettlementController.triggerBatch(request))
                .isInstanceOf(BatchAlreadyRunningException.class);
        }
    }

    private Page<SettlementResponse> createMockSettlementResponsePage() {
        return new PageImpl<>(
            List.of(createMockSettlementResponse()),
            PageRequest.of(0, 20, Sort.by("createdAt").descending()),
            1
        );
    }

    private SettlementResponse createMockSettlementResponse() {
        return new SettlementResponse(1L, 1L, "SELLER001", "테스트 판매자",
            com.company.settlement.domain.enums.CycleType.DAILY,
            LocalDate.now().minusDays(1), LocalDate.now().minusDays(1),
            new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("10000"),
            BigDecimal.ZERO, new BigDecimal("90000"),
            com.company.settlement.domain.enums.SettlementStatus.CONFIRMED, LocalDateTime.now());
    }

    private SettlementDetailResponse createMockSettlementDetailResponse() {
        var seller = new com.company.settlement.dto.response.SellerSummaryResponse(
            1L, "SELLER001", "테스트 판매자", new BigDecimal("0.1000"),
            com.company.settlement.domain.enums.SellerStatus.ACTIVE);
        return new SettlementDetailResponse(1L, seller,
            com.company.settlement.domain.enums.CycleType.DAILY,
            LocalDate.now().minusDays(1), LocalDate.now().minusDays(1),
            new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"),
            new BigDecimal("0.1000"), new BigDecimal("10000"), BigDecimal.ZERO,
            BigDecimal.ZERO, new BigDecimal("90000"),
            com.company.settlement.domain.enums.SettlementStatus.CONFIRMED, List.of(),
            LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    private SettlementStatisticsResponse createMockStatisticsResponse() {
        return new SettlementStatisticsResponse(LocalDate.now().minusDays(7), LocalDate.now(),
            10L, 2L, 6L, 2L, 0L,
            new BigDecimal("1000000"), new BigDecimal("50000"), new BigDecimal("95000"),
            BigDecimal.ZERO, new BigDecimal("855000"));
    }

    private BatchTriggerResponse createMockBatchTriggerResponse() {
        return new BatchTriggerResponse(123L, "dailySettlementJob", LocalDate.now().minusDays(1),
            "STARTED", LocalDateTime.now(), "배치 작업이 성공적으로 시작되었습니다.");
    }
}
