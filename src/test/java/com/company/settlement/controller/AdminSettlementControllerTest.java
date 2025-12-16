package com.company.settlement.controller;

import com.company.settlement.dto.request.BatchTriggerRequest;
import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.dto.response.SettlementStatisticsResponse;
import com.company.settlement.exception.BatchAlreadyRunningException;
import com.company.settlement.exception.SettlementNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminSettlementController API 통합 테스트
 * - 관리자용 정산 API 엔드포인트 테스트
 * - 배치 실행 트리거 기능 테스트
 * - 통계 집계 기능 테스트
 */
@DisplayName("AdminSettlementController API 테스트")
class AdminSettlementControllerTest extends AbstractControllerTest {

    @Nested
    @DisplayName("GET /api/admin/settlements - 전체 정산 목록 조회")
    class GetAllSettlements {

        @Test
        @DisplayName("성공: 전체 정산 목록 조회")
        void getAllSettlements_Success() throws Exception {
            // given
            Page<SettlementResponse> mockResponse = TestDataBuilder.createSettlementResponsePage();

            when(adminSettlementService.getAllSettlements(any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when & then
            var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements"));

            result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].sellerId").exists())
                .andExpect(jsonPath("$.content[0].sellerCode").value("SELLER001"))
                .andExpect(jsonPath("$.content[0].payoutAmount").value(90000))
                .andExpect(jsonPath("$.totalElements").value(1));

            SettlementAssertions.assertThatPageMetadataIsValid(result, 20, 1);
        }

        @Test
        @DisplayName("성공: 빈 정산 목록 조회")
        void getAllSettlements_EmptyResult() throws Exception {
            // given
            Page<SettlementResponse> emptyResponse = TestDataBuilder.createEmptySettlementResponsePage();

            when(adminSettlementService.getAllSettlements(any(PageRequest.class)))
                .thenReturn(emptyResponse);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 20, 50, 100})
        @DisplayName("성공: 다양한 페이지 크기로 조회")
        void getAllSettlements_DifferentPageSizes(int pageSize) throws Exception {
            // given
            Page<SettlementResponse> mockResponse = TestDataBuilder.createSettlementResponsePage();

            when(adminSettlementService.getAllSettlements(any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements")
                    .param("size", String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(pageSize));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/settlements/period - 기간별 정산 목록 조회")
    class GetSettlementsByPeriod {

        @Test
        @DisplayName("성공: 유효한 기간으로 정산 목록 조회")
        void getSettlementsByPeriod_Success() throws Exception {
            // given
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);
            Page<SettlementResponse> mockResponse = TestDataBuilder.createSettlementResponsePage();

            when(adminSettlementService.getSettlementsByPeriod(
                eq(startDate), eq(endDate), any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/period")
                    .param("start", "2024-01-01")
                    .param("end", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].sellerCode").value("SELLER001"));
        }

        @Test
        @DisplayName("성공: 같은 날짜 기간으로 조회")
        void getSettlementsByPeriod_SameDay() throws Exception {
            // given
            LocalDate date = LocalDate.of(2024, 1, 15);
            Page<SettlementResponse> mockResponse = TestDataBuilder.createSettlementResponsePage();

            when(adminSettlementService.getSettlementsByPeriod(
                eq(date), eq(date), any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/period")
                    .param("start", "2024-01-15")
                    .param("end", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("실패: start 날짜 누락")
        void getSettlementsByPeriod_MissingStartDate() throws Exception {
            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/period")
                    .param("end", "2024-01-31"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: end 날짜 누락")
        void getSettlementsByPeriod_MissingEndDate() throws Exception {
            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/period")
                    .param("start", "2024-01-01"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 잘못된 날짜 범위 (start > end)")
        void getSettlementsByPeriod_InvalidDateRange() throws Exception {
            // given
            LocalDate startDate = LocalDate.of(2024, 2, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);

            when(adminSettlementService.getSettlementsByPeriod(
                eq(startDate), eq(endDate), any(PageRequest.class)))
                .thenThrow(new IllegalArgumentException("Start date cannot be after end date"));

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/period")
                    .param("start", "2024-02-01")
                    .param("end", "2024-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Start date cannot be after end date"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid-date", "2024-13-01", "2024-02-30"})
        @DisplayName("실패: 잘못된 날짜 형식")
        void getSettlementsByPeriod_InvalidDateFormat(String invalidDate) throws Exception {
            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/period")
                    .param("start", invalidDate)
                    .param("end", "2024-01-31"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/settlements/{settlementId} - 관리자용 정산 상세 조회")
    class GetSettlementDetail {

        @Test
        @DisplayName("성공: 정산 상세 조회")
        void getSettlementDetail_Success() throws Exception {
            // given
            Long settlementId = 1L;
            var mockResponse = TestDataBuilder.createSettlementDetailResponse();

            when(adminSettlementService.getSettlementDetail(eq(settlementId)))
                .thenReturn(mockResponse);

            // when & then
            var result = mockMvc.perform(MockMvcRequestBuilders
                    .get("/api/admin/settlements/{settlementId}", settlementId));

            result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(settlementId))
                .andExpect(jsonPath("$.seller.id").value(1))
                .andExpect(jsonPath("$.payoutAmount").value(90000))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].settlementAmount").value(45000));

            SettlementAssertions.assertThatSettlementDetailResponseIsValid(result);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 정산 ID")
        void getSettlementDetail_NotFound() throws Exception {
            // given
            Long nonExistentId = 999L;
            String errorMessage = "Settlement not found: " + nonExistentId;

            when(adminSettlementService.getSettlementDetail(eq(nonExistentId)))
                .thenThrow(new SettlementNotFoundException(errorMessage));

            // when & then
            var result = mockMvc.perform(MockMvcRequestBuilders
                    .get("/api/admin/settlements/{settlementId}", nonExistentId));

            SettlementAssertions.assertThatErrorResponseIsValid(result, 404);
            result.andExpect(jsonPath("$.detail").value(errorMessage));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 settlementId 형식")
        void getSettlementDetail_InvalidSettlementId() throws Exception {
            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/invalid-id"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/settlements/statistics - 정산 통계 조회")
    class GetSettlementStatistics {

        @Test
        @DisplayName("성공: 정산 통계 조회")
        void getSettlementStatistics_Success() throws Exception {
            // given
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);
            var mockResponse = TestDataBuilder.createSettlementStatisticsResponse();

            when(adminSettlementService.getStatistics(eq(startDate), eq(endDate)))
                .thenReturn(mockResponse);

            // when & then
            var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/statistics")
                    .param("start", "2024-01-01")
                    .param("end", "2024-01-31"));

            result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodStart").value("2024-01-01"))
                .andExpect(jsonPath("$.periodEnd").value("2024-01-31"))
                .andExpect(jsonPath("$.totalSettlementCount").value(10))
                .andExpect(jsonPath("$.pendingCount").value(2))
                .andExpect(jsonPath("$.confirmedCount").value(6))
                .andExpect(jsonPath("$.paidCount").value(2))
                .andExpect(jsonPath("$.cancelledCount").value(0))
                .andExpect(jsonPath("$.totalSalesAmount").value(1000000))
                .andExpect(jsonPath("$.totalRefundAmount").value(50000))
                .andExpect(jsonPath("$.totalCommissionAmount").value(95000))
                .andExpect(jsonPath("$.totalVatAmount").value(0))
                .andExpect(jsonPath("$.totalSettlementAmount").value(855000));

            SettlementAssertions.assertThatStatisticsResponseIsValid(result);
        }

        @Test
        @DisplayName("성공: 통계가 없는 기간 조회")
        void getSettlementStatistics_NoData() throws Exception {
            // given
            LocalDate startDate = LocalDate.of(2024, 12, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            var emptyResponse = new SettlementStatisticsResponse(
                startDate,
                endDate,
                0L, 0L, 0L, 0L, 0L,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO
            );

            when(adminSettlementService.getStatistics(eq(startDate), eq(endDate)))
                .thenReturn(emptyResponse);

            // when & then
            var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/statistics")
                    .param("start", "2024-12-01")
                    .param("end", "2024-12-31"));

            result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSettlementCount").value(0))
                .andExpect(jsonPath("$.totalSalesAmount").value(0));

            SettlementAssertions.assertThatStatisticsResponseIsValid(result);
        }

        @Test
        @DisplayName("실패: 날짜 파라미터 누락")
        void getSettlementStatistics_MissingParameters() throws Exception {
            // when & then - start 날짜 누락
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/statistics")
                    .param("end", "2024-01-31"))
                .andExpect(status().isBadRequest());

            // when & then - end 날짜 누락
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/statistics")
                    .param("start", "2024-01-01"))
                .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @CsvSource({
            "2024-02-01, 2024-01-31, Start date cannot be after end date",
            "2024-01-31, 2024-01-01, Start date cannot be after end date"
        })
        @DisplayName("실패: 잘못된 날짜 범위")
        void getSettlementStatistics_InvalidDateRange(String start, String end, String expectedError) throws Exception {
            // given
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);

            when(adminSettlementService.getStatistics(eq(startDate), eq(endDate)))
                .thenThrow(new IllegalArgumentException(expectedError));

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/settlements/statistics")
                    .param("start", start)
                    .param("end", end))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(expectedError));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/settlements/batch/trigger - 배치 수동 실행")
    class TriggerBatch {

        @Test
        @DisplayName("성공: 유효한 날짜로 배치 실행")
        void triggerBatch_Success() throws Exception {
            // given
            LocalDate targetDate = LocalDate.now().minusDays(1);
            BatchTriggerRequest request = new BatchTriggerRequest(targetDate);
            var mockResponse = TestDataBuilder.createBatchTriggerResponse();

            when(batchTriggerService.triggerSettlementBatch(eq(targetDate)))
                .thenReturn(mockResponse);

            // when & then
            var result = mockMvc.perform(MockMvcRequestBuilders
                    .post("/api/admin/settlements/batch/trigger")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").value(123))
                .andExpect(jsonPath("$.jobName").value("dailySettlementJob"))
                .andExpect(jsonPath("$.targetDate").value(targetDate.toString()))
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.message").value("배치 작업이 성공적으로 시작되었습니다."));

            SettlementAssertions.assertThatBatchTriggerResponseIsValid(result);
        }

        @Test
        @DisplayName("실패: 미래 날짜로 배치 실행 시도")
        void triggerBatch_FutureDate() throws Exception {
            // given
            LocalDate futureDate = LocalDate.now().plusDays(1);
            BatchTriggerRequest request = new BatchTriggerRequest(futureDate);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                    .post("/api/admin/settlements/batch/trigger")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("실패: null 날짜로 배치 실행 시도")
        void triggerBatch_NullDate() throws Exception {
            // given
            String invalidRequest = "{\"targetDate\":null}";

            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                    .post("/api/admin/settlements/batch/trigger")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("실패: 이미 실행 중인 배치")
        void triggerBatch_BatchAlreadyRunning() throws Exception {
            // given
            LocalDate targetDate = LocalDate.now().minusDays(1);
            BatchTriggerRequest request = new BatchTriggerRequest(targetDate);
            String errorMessage = "Batch job is already running: dailySettlementJob";

            when(batchTriggerService.triggerSettlementBatch(eq(targetDate)))
                .thenThrow(new BatchAlreadyRunningException(errorMessage));

            // when & then
            var result = mockMvc.perform(MockMvcRequestBuilders
                    .post("/api/admin/settlements/batch/trigger")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            SettlementAssertions.assertThatErrorResponseIsValid(result, 409);
            result.andExpect(jsonPath("$.detail").value(errorMessage));
        }

        @Test
        @DisplayName("실패: 잘못된 JSON 형식")
        void triggerBatch_InvalidJson() throws Exception {
            // given
            String invalidJson = "{targetDate: '2024-01-01'}"; // 따�표 누락

            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                    .post("/api/admin/settlements/batch/trigger")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: Content-Type 누락")
        void triggerBatch_MissingContentType() throws Exception {
            // given
            LocalDate targetDate = LocalDate.now().minusDays(1);
            BatchTriggerRequest request = new BatchTriggerRequest(targetDate);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                    .post("/api/admin/settlements/batch/trigger")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: Request body 누락")
        void triggerBatch_MissingRequestBody() throws Exception {
            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                    .post("/api/admin/settlements/batch/trigger")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
    }
}