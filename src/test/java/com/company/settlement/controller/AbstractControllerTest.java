package com.company.settlement.controller;

// 직접 AbstractBatchTest를 상속받지 않고 Testcontainers 설정을 복제
import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.domain.enums.CycleType;
import com.company.settlement.domain.enums.SellerStatus;
import com.company.settlement.domain.enums.SettlementItemType;
import com.company.settlement.domain.enums.SettlementSource;
import com.company.settlement.domain.enums.SettlementStatus;
import com.company.settlement.dto.request.BatchTriggerRequest;
import com.company.settlement.dto.response.*;
import com.company.settlement.service.AdminSettlementService;
import com.company.settlement.service.BatchTriggerService;
import com.company.settlement.service.SettlementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API 통합 테스트를 위한 추상 베이스 클래스
 * - Testcontainers MySQL 환경 설정 공유
 * - MockMvc 및 ObjectMapper 공통 설정
 * - Service 계층 Mock 빈 설정
 * - 테스트 데이터 생성 유틸리티 제공
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3307/settlement_test?characterEncoding=UTF-8&serverTimezone=Asia/Seoul",
    "spring.datasource.username=root",
    "spring.datasource.password=test1234",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected SettlementService settlementService;

    @MockBean
    protected AdminSettlementService adminSettlementService;

    @MockBean
    protected BatchTriggerService batchTriggerService;

    // 테스트 데이터
    protected Seller testSeller;
    protected Settlement testSettlement;
    protected List<Settlement> testSettlements;

    @BeforeEach
    void setUpTestData() {
        // ObjectMapper 설정
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 기본 테스트 데이터 생성
        testSeller = TestDataBuilder.createTestSeller();
        testSettlement = TestDataBuilder.createTestSettlement(testSeller);
        testSettlements = Arrays.asList(testSettlement);
    }

    /**
     * 인증 헤더를 포함한 요청 실행을 위한 유틸리티 클래스
     */
    protected static class MockAuth {
        public static final String HEADER_NAME = "X-Seller-Id";
        public static final Long VALID_SELLER_ID = 1L;
        public static final Long INVALID_SELLER_ID = 999L;

        /**
         * 판매자 인증 헤더를 추가하여 요청 실행
         */
        public static MockHttpServletRequestBuilder withSellerAuth(MockHttpServletRequestBuilder request) {
            return request.header(HEADER_NAME, VALID_SELLER_ID);
        }

        /**
         * 특정 판매자 ID로 인증 헤더 추가하여 요청 실행
         */
        public static MockHttpServletRequestBuilder withSellerAuth(MockHttpServletRequestBuilder request, Long sellerId) {
            return request.header(HEADER_NAME, sellerId);
        }
    }

    /**
     * 테스트 데이터 생성을 위한 빌더 클래스
     */
    protected static class TestDataBuilder {

        public static Seller createTestSeller() {
            return Seller.builder()
                .sellerCode("SELLER001")
                .sellerName("테스트 판매자")
                .commissionRate(new BigDecimal("0.1000")) // 10% 수수료
                .status(SellerStatus.ACTIVE)
                .build();
        }

        public static Settlement createTestSettlement(Seller seller) {
            return Settlement.builder()
                .seller(seller)
                .cycleType(CycleType.DAILY)
                .periodStart(LocalDate.now().minusDays(1))
                .periodEnd(LocalDate.now().minusDays(1))
                .grossSalesAmount(new BigDecimal("100000")) // 총 판매액
                .refundAmount(BigDecimal.ZERO) // 환불액
                .commissionRate(new BigDecimal("0.1000")) // 수수료율
                .commissionAmount(new BigDecimal("10000")) // 수수료
                .taxAmount(BigDecimal.ZERO) // 부가세
                .adjustmentAmount(BigDecimal.ZERO) // 조정금액
                .payoutAmount(new BigDecimal("90000")) // 정산금액
                .status(SettlementStatus.CONFIRMED)
                .build();
        }

        public static List<SettlementItemResponse> createSettlementItemResponses() {
            return Arrays.asList(
                new SettlementItemResponse(
                    1L,
                    SettlementItemType.SALE,
                    SettlementSource.ORDER_ITEM,
                    1L,
                    new BigDecimal("50000"),
                    new BigDecimal("0.1000"),
                    new BigDecimal("5000"),
                    new BigDecimal("45000"),
                    "판매 정산",
                    LocalDateTime.now()
                ),
                new SettlementItemResponse(
                    2L,
                    SettlementItemType.SALE,
                    SettlementSource.ORDER_ITEM,
                    2L,
                    new BigDecimal("50000"),
                    new BigDecimal("0.1000"),
                    new BigDecimal("5000"),
                    new BigDecimal("45000"),
                    "판매 정산",
                    LocalDateTime.now()
                )
            );
        }

        public static SettlementResponse createSettlementResponse() {
            return new SettlementResponse(
                1L,
                1L,
                "SELLER001",
                "테스트 판매자",
                CycleType.DAILY,
                LocalDate.now().minusDays(1),
                LocalDate.now().minusDays(1),
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                new BigDecimal("90000"),
                SettlementStatus.CONFIRMED,
                LocalDateTime.now()
            );
        }

        public static SettlementDetailResponse createSettlementDetailResponse() {
            SellerSummaryResponse seller = new SellerSummaryResponse(
                1L,
                "SELLER001",
                "테스트 판매자",
                new BigDecimal("0.1000"),
                SellerStatus.ACTIVE
            );

            List<SettlementItemResponse> items = createSettlementItemResponses();

            return new SettlementDetailResponse(
                1L,
                seller,
                CycleType.DAILY,
                LocalDate.now().minusDays(1),
                LocalDate.now().minusDays(1),
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                new BigDecimal("100000"),
                new BigDecimal("0.1000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("90000"),
                SettlementStatus.CONFIRMED,
                items,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        }

        public static SettlementStatisticsResponse createSettlementStatisticsResponse() {
            return new SettlementStatisticsResponse(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                10L,                    // totalSettlementCount
                2L,                     // pendingCount
                6L,                     // confirmedCount
                2L,                     // paidCount
                0L,                     // cancelledCount
                new BigDecimal("1000000"), // totalSalesAmount
                new BigDecimal("50000"),   // totalRefundAmount
                new BigDecimal("95000"),   // totalCommissionAmount
                BigDecimal.ZERO,           // totalVatAmount
                new BigDecimal("855000")   // totalSettlementAmount
            );
        }

        public static BatchTriggerResponse createBatchTriggerResponse() {
            return new BatchTriggerResponse(
                123L,
                "dailySettlementJob",
                LocalDate.now().minusDays(1),
                "STARTED",
                LocalDateTime.now(),
                "배치 작업이 성공적으로 시작되었습니다."
            );
        }

        public static Page<SettlementResponse> createSettlementResponsePage() {
            List<SettlementResponse> content = Arrays.asList(createSettlementResponse());
            PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            return new PageImpl<>(content, pageRequest, 1);
        }

        public static Page<SettlementResponse> createEmptySettlementResponsePage() {
            PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            return new PageImpl<>(List.of(), pageRequest, 0);
        }
    }

    /**
     * API 응답 검증을 위한 커스텀 Assertion 유틸리티
     */
    protected static class SettlementAssertions {

        public static void assertThatSettlementResponseIsValid(ResultActions result, SettlementResponse expected) throws Exception {
            result
                .andExpect(jsonPath("$.id").value(expected.id()))
                .andExpect(jsonPath("$.sellerId").value(expected.sellerId()))
                .andExpect(jsonPath("$.sellerCode").value(expected.sellerCode()))
                .andExpect(jsonPath("$.sellerName").value(expected.sellerName()))
                .andExpect(jsonPath("$.cycleType").value(expected.cycleType().toString()))
                .andExpect(jsonPath("$.grossSalesAmount").value(expected.grossSalesAmount().doubleValue()))
                .andExpect(jsonPath("$.refundAmount").value(expected.refundAmount().doubleValue()))
                .andExpect(jsonPath("$.commissionAmount").value(expected.commissionAmount().doubleValue()))
                .andExpect(jsonPath("$.payoutAmount").value(expected.payoutAmount().doubleValue()))
                .andExpect(jsonPath("$.status").value(expected.status().toString()));
        }

        public static void assertThatPageMetadataIsValid(ResultActions result, int expectedSize, long totalElements) throws Exception {
            result
                .andExpect(jsonPath("$.size").value(expectedSize))
                .andExpect(jsonPath("$.totalElements").value(totalElements))
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").isBoolean())
                .andExpect(jsonPath("$.last").isBoolean())
                .andExpect(jsonPath("$.numberOfElements").isNumber());
        }

        public static void assertThatSettlementDetailResponseIsValid(ResultActions result) throws Exception {
            result
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.seller.id").isNumber())
                .andExpect(jsonPath("$.seller.sellerCode").isString())
                .andExpect(jsonPath("$.cycleType").isString())
                .andExpect(jsonPath("$.payoutAmount").isNumber())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].settlementAmount").isNumber());
        }

        public static void assertThatStatisticsResponseIsValid(ResultActions result) throws Exception {
            result
                .andExpect(jsonPath("$.periodStart").isString())
                .andExpect(jsonPath("$.periodEnd").isString())
                .andExpect(jsonPath("$.totalSettlementCount").isNumber())
                .andExpect(jsonPath("$.pendingCount").isNumber())
                .andExpect(jsonPath("$.confirmedCount").isNumber())
                .andExpect(jsonPath("$.paidCount").isNumber())
                .andExpect(jsonPath("$.cancelledCount").isNumber())
                .andExpect(jsonPath("$.totalSalesAmount").isNumber())
                .andExpect(jsonPath("$.totalSettlementAmount").isNumber());
        }

        public static void assertThatBatchTriggerResponseIsValid(ResultActions result) throws Exception {
            result
                .andExpect(jsonPath("$.jobExecutionId").isNumber())
                .andExpect(jsonPath("$.jobName").isString())
                .andExpect(jsonPath("$.targetDate").isString())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.startTime").isString())
                .andExpect(jsonPath("$.message").isString());
        }

        public static void assertThatErrorResponseIsValid(ResultActions result, int expectedStatus) throws Exception {
            result
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.type").isString())
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").isString());
        }
    }
}