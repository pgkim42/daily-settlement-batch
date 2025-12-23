package com.company.settlement.controller;

import com.company.settlement.dto.response.SettlementDetailResponse;
import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.exception.SettlementAccessDeniedException;
import com.company.settlement.exception.SettlementNotFoundException;
import com.company.settlement.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SettlementController 단위 테스트
 * - 판매자용 정산 API 엔드포인트 테스트
 * - 서비스 계층 목킹으로 Controller 로직만 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementController 단위 테스트")
class SettlementControllerTest {

    @Mock
    private SettlementService settlementService;

    @InjectMocks
    private SettlementController settlementController;

    @Nested
    @DisplayName("GET /api/settlements - 내 정산 목록 조회")
    class GetMySettlements {

        @Test
        @DisplayName("성공: 정상적인 정산 목록 조회")
        void getMySettlements_Success() {
            // given
            Long sellerId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            Page<SettlementResponse> mockResponse = createMockSettlementResponsePage();

            when(settlementService.getMySettlements(eq(sellerId), any(PageRequest.class)))
                .thenReturn(mockResponse);

            // when
            ResponseEntity<Page<SettlementResponse>> response = settlementController.getMySettlements(sellerId, pageRequest);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).sellerId()).isEqualTo(sellerId);
            assertThat(response.getBody().getContent().get(0).sellerCode()).isEqualTo("SELLER001");

            verify(settlementService).getMySettlements(eq(sellerId), any(PageRequest.class));
        }

        @Test
        @DisplayName("성공: 빈 정산 목록 조회")
        void getMySettlements_EmptyResult() {
            // given
            Long sellerId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<SettlementResponse> emptyResponse = Page.empty();

            when(settlementService.getMySettlements(eq(sellerId), any(PageRequest.class)))
                .thenReturn(emptyResponse);

            // when
            ResponseEntity<Page<SettlementResponse>> response = settlementController.getMySettlements(sellerId, pageRequest);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 10})
        @DisplayName("성공: 다양한 페이지 번호로 조회")
        void getMySettlements_DifferentPageNumbers(int pageNumber) {
            // given
            Long sellerId = 1L;
            PageRequest pageRequest = PageRequest.of(pageNumber, 20);

            when(settlementService.getMySettlements(eq(sellerId), any(PageRequest.class)))
                .thenAnswer(invocation -> {
                    PageRequest pr = invocation.getArgument(1);
                    return new PageImpl<>(List.of(createMockSettlementResponse()), pr, 1);
                });

            // when
            ResponseEntity<Page<SettlementResponse>> response = settlementController.getMySettlements(sellerId, pageRequest);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getNumber()).isEqualTo(pageNumber);
        }
    }

    @Nested
    @DisplayName("GET /api/settlements/{settlementId} - 정산 상세 조회")
    class GetSettlementDetail {

        @Test
        @DisplayName("성공: 정상적인 정산 상세 조회")
        void getSettlementDetail_Success() {
            // given
            Long sellerId = 1L;
            Long settlementId = 1L;
            SettlementDetailResponse mockResponse = createMockSettlementDetailResponse();

            when(settlementService.getSettlementDetail(eq(sellerId), eq(settlementId)))
                .thenReturn(mockResponse);

            // when
            ResponseEntity<SettlementDetailResponse> response = settlementController.getSettlementDetail(sellerId, settlementId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().id()).isEqualTo(settlementId);
            assertThat(response.getBody().seller().id()).isEqualTo(sellerId);
            assertThat(response.getBody().seller().sellerCode()).isEqualTo("SELLER001");
            assertThat(response.getBody().payoutAmount()).isEqualByComparingTo("90000");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 정산 ID")
        void getSettlementDetail_NotFound() {
            // given
            Long sellerId = 1L;
            Long nonExistentId = 999L;
            String errorMessage = "Settlement not found: " + nonExistentId;

            when(settlementService.getSettlementDetail(eq(sellerId), eq(nonExistentId)))
                .thenThrow(new SettlementNotFoundException(errorMessage));

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> settlementController.getSettlementDetail(sellerId, nonExistentId))
                .isInstanceOf(SettlementNotFoundException.class)
                .hasMessageContaining(errorMessage);
        }

        @Test
        @DisplayName("실패: 다른 판매자의 정산 접근")
        void getSettlementDetail_AccessDenied() {
            // given
            Long sellerId = 1L;
            Long settlementId = 2L;

            when(settlementService.getSettlementDetail(eq(sellerId), eq(settlementId)))
                .thenThrow(new SettlementAccessDeniedException(sellerId, settlementId));

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> settlementController.getSettlementDetail(sellerId, settlementId))
                .isInstanceOf(SettlementAccessDeniedException.class);
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
            java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now().minusDays(1),
            new java.math.BigDecimal("100000"), java.math.BigDecimal.ZERO, new java.math.BigDecimal("10000"),
            java.math.BigDecimal.ZERO, new java.math.BigDecimal("90000"),
            com.company.settlement.domain.enums.SettlementStatus.CONFIRMED, java.time.LocalDateTime.now());
    }

    private SettlementDetailResponse createMockSettlementDetailResponse() {
        var seller = new com.company.settlement.dto.response.SellerSummaryResponse(
            1L, "SELLER001", "테스트 판매자", new java.math.BigDecimal("0.1000"),
            com.company.settlement.domain.enums.SellerStatus.ACTIVE);
        return new SettlementDetailResponse(1L, seller,
            com.company.settlement.domain.enums.CycleType.DAILY,
            java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now().minusDays(1),
            new java.math.BigDecimal("100000"), java.math.BigDecimal.ZERO, new java.math.BigDecimal("100000"),
            new java.math.BigDecimal("0.1000"), new java.math.BigDecimal("10000"), java.math.BigDecimal.ZERO,
            java.math.BigDecimal.ZERO, new java.math.BigDecimal("90000"),
            com.company.settlement.domain.enums.SettlementStatus.CONFIRMED, List.of(),
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }
}
