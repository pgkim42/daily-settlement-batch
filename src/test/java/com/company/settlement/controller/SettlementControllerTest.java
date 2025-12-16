package com.company.settlement.controller;

import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.exception.SettlementAccessDeniedException;
import com.company.settlement.exception.SettlementNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SettlementController API 통합 테스트
 * - 판매자용 정산 API 엔드포인트 테스트
 * - 인증 헤더(X-Seller-Id) 처리 검증
 * - 예외 상황 및 에러 응답 검증
 */
@DisplayName("SettlementController API 테스트")
class SettlementControllerTest extends AbstractControllerTest {

    @Nested
    @DisplayName("GET /api/settlements - 내 정산 목록 조회")
    class GetMySettlements {

        @Test
        @DisplayName("성공: 정상적인 정산 목록 조회")
        void getMySettlements_Success() throws Exception {
            // given
            Page<SettlementResponse> mockResponse = TestDataBuilder.createSettlementResponsePage();

            when(settlementService.getMySettlements(eq(MockAuth.VALID_SELLER_ID), any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when & then
            var result = mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements"))
                    .param("page", "0")
                    .param("size", "20")
                    .param("sort", "createdAt,desc")
            );

            result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].sellerId").value(MockAuth.VALID_SELLER_ID))
                .andExpect(jsonPath("$.content[0].sellerCode").value("SELLER001"))
                .andExpect(jsonPath("$.content[0].payoutAmount").value(90000))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20));

            SettlementAssertions.assertThatPageMetadataIsValid(result, 20, 1);
            SettlementAssertions.assertThatSettlementResponseIsValid(result, TestDataBuilder.createSettlementResponse());
        }

        @Test
        @DisplayName("성공: 빈 정산 목록 조회")
        void getMySettlements_EmptyResult() throws Exception {
            // given
            Page<SettlementResponse> emptyResponse = TestDataBuilder.createEmptySettlementResponsePage();

            when(settlementService.getMySettlements(eq(MockAuth.VALID_SELLER_ID), any(PageRequest.class)))
                .thenReturn(emptyResponse);

            // when & then
            var result = mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements"))
            );

            result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isArray());

            SettlementAssertions.assertThatPageMetadataIsValid(result, 20, 0);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 10})
        @DisplayName("성공: 다양한 페이지 번호로 조회")
        void getMySettlements_DifferentPageNumbers(int pageNumber) throws Exception {
            // given
            Page<SettlementResponse> mockResponse = TestDataBuilder.createSettlementResponsePage();

            when(settlementService.getMySettlements(eq(MockAuth.VALID_SELLER_ID), any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when & then
            mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements"))
                    .param("page", String.valueOf(pageNumber))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.number").value(pageNumber));
        }

        @Test
        @DisplayName("실패: 인증 헤더 누락")
        void getMySettlements_MissingAuthHeader() throws Exception {
            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/settlements"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Required request header 'X-Seller-Id' is not present"));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 판매자 ID")
        void getMySettlements_InvalidSellerId() throws Exception {
            // given
            when(settlementService.getMySettlements(eq(MockAuth.INVALID_SELLER_ID), any(PageRequest.class)))
                .thenReturn(TestDataBuilder.createEmptySettlementResponsePage());

            // when & then
            mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements"), MockAuth.INVALID_SELLER_ID)
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/settlements/{settlementId} - 정산 상세 조회")
    class GetSettlementDetail {

        @Test
        @DisplayName("성공: 정상적인 정산 상세 조회")
        void getSettlementDetail_Success() throws Exception {
            // given
            Long settlementId = 1L;
            var mockResponse = TestDataBuilder.createSettlementDetailResponse();

            when(settlementService.getSettlementDetail(eq(MockAuth.VALID_SELLER_ID), eq(settlementId)))
                .thenReturn(mockResponse);

            // when & then
            var result = mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements/{settlementId}", settlementId))
            );

            result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(settlementId))
                .andExpect(jsonPath("$.seller.id").value(MockAuth.VALID_SELLER_ID))
                .andExpect(jsonPath("$.seller.sellerCode").value("SELLER001"))
                .andExpect(jsonPath("$.payoutAmount").value(90000))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].settlementAmount").value(45000))
                .andExpect(jsonPath("$.items.length()").value(2));

            SettlementAssertions.assertThatSettlementDetailResponseIsValid(result);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 정산 ID")
        void getSettlementDetail_NotFound() throws Exception {
            // given
            Long nonExistentId = 999L;
            String errorMessage = "Settlement not found: " + nonExistentId;

            when(settlementService.getSettlementDetail(eq(MockAuth.VALID_SELLER_ID), eq(nonExistentId)))
                .thenThrow(new SettlementNotFoundException(errorMessage));

            // when & then
            var result = mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements/{settlementId}", nonExistentId))
            );

            SettlementAssertions.assertThatErrorResponseIsValid(result, 404);
            result.andExpect(jsonPath("$.detail").value(errorMessage));
        }

        @Test
        @DisplayName("실패: 다른 판매자의 정산 접근")
        void getSettlementDetail_AccessDenied() throws Exception {
            // given
            Long settlementId = 2L;
            when(settlementService.getSettlementDetail(eq(MockAuth.VALID_SELLER_ID), eq(settlementId)))
                .thenThrow(new SettlementAccessDeniedException(MockAuth.VALID_SELLER_ID, settlementId));

            // when & then
            var result = mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements/{settlementId}", settlementId))
            );

            SettlementAssertions.assertThatErrorResponseIsValid(result, 403);
        }

        @Test
        @DisplayName("실패: 인증 헤더 누락")
        void getSettlementDetail_MissingAuthHeader() throws Exception {
            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/settlements/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Required request header 'X-Seller-Id' is not present"));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 settlementId 형식")
        void getSettlementDetail_InvalidSettlementId() throws Exception {
            // when & then
            mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements/invalid-id"))
            )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("인증 및 권한 검증")
    class AuthenticationTests {

        @Test
        @DisplayName("X-Seller-Id 헤더가 있는 경우에만 API 접근 가능")
        void onlyAuthenticatedAccess_Allowed() throws Exception {
            // given
            when(settlementService.getMySettlements(eq(MockAuth.VALID_SELLER_ID), any(PageRequest.class)))
                .thenReturn(TestDataBuilder.createSettlementResponsePage());

            // when & then - 헤더가 있는 경우 성공
            mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements"))
            )
                .andExpect(status().isOk());

            // when & then - 헤더가 없는 경우 실패
            mockMvc.perform(MockMvcRequestBuilders.get("/api/settlements"))
                .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {"1", "999", "0", "-1"})
        @DisplayName("다양한 판매자 ID 값으로 요청 시도")
        void differentSellerId_Requests(String sellerId) throws Exception {
            // given
            Long id = Long.parseLong(sellerId);
            when(settlementService.getMySettlements(eq(id), any(PageRequest.class)))
                .thenReturn(TestDataBuilder.createEmptySettlementResponsePage());

            // when & then
            mockMvc.perform(
                MockAuth.withSellerAuth(MockMvcRequestBuilders.get("/api/settlements"), id)
            )
                .andExpect(status().isOk());
        }
    }
}