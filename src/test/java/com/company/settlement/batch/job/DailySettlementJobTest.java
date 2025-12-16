package com.company.settlement.batch.job;

import com.company.settlement.batch.AbstractBatchTest;
import com.company.settlement.domain.entity.*;
import com.company.settlement.domain.enums.*;
import com.company.settlement.repository.*;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DailySettlementJob 통합 테스트
 * 
 * 테스트 포인트:
 * - 정상 배치 실행 및 정산 데이터 생성
 * - 멱등성 검증 (중복 실행 시 Skip)
 * - Fault Tolerant 동작 (일부 실패 시 계속 진행)
 * - Chunk 단위 트랜잭션 검증
 */
class DailySettlementJobTest extends AbstractBatchTest {

    @Autowired
    private Job dailySettlementJob;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Seller testSeller1;
    private Seller testSeller2;
    private Seller testSeller3;

    @BeforeEach
    void setUp() {
        // Job 설정
        setJob(dailySettlementJob);

        // 기존 데이터 정리 (트랜잭션 내에서 실행)
        transactionTemplate.execute(status -> {
            settlementRepository.deleteAll();
            refundRepository.deleteAll();
            paymentRepository.deleteAll();
            orderRepository.deleteAll();
            sellerRepository.deleteAll();
            return null;
        });

        // 테스트 판매자 생성 (트랜잭션 내에서 실행)
        transactionTemplate.execute(status -> {
            testSeller1 = createSeller("SELLER001", "판매자1", SellerStatus.ACTIVE);
            testSeller2 = createSeller("SELLER002", "판매자2", SellerStatus.ACTIVE);
            testSeller3 = createSeller("SELLER003", "판매자3", SellerStatus.INACTIVE);
            return null;
        });
    }

    @Test
    @DisplayName("배치 정상 실행 - 활성 판매자 정산 생성")
    void executeJob_Success() throws Exception {
        // given
        LocalDate targetDate = LocalDate.of(2024, 1, 15);
        transactionTemplate.execute(status -> {
            createOrdersForSeller(testSeller1, targetDate, 3);
            createOrdersForSeller(testSeller2, targetDate, 2);
            return null;
        });

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // 활성 판매자 2명에 대한 정산 생성 확인
        transactionTemplate.execute(status -> {
            List<Settlement> settlements = settlementRepository.findAll();
            assertThat(settlements).hasSize(2);
            
            // Fetch Join으로 Seller 조회
            settlements.forEach(s -> Hibernate.initialize(s.getSeller()));
            assertThat(settlements)
                    .extracting(s -> s.getSeller().getSellerCode())
                    .containsExactlyInAnyOrder("SELLER001", "SELLER002");

            // 비활성 판매자는 정산 생성 안됨
            assertThat(settlements)
                    .extracting(s -> s.getSeller().getSellerCode())
                    .doesNotContain("SELLER003");
            return null;
        });
    }

    @Test
    @DisplayName("멱등성 검증 - 동일 기간 중복 실행 시 Skip")
    void executeJob_Idempotency() throws Exception {
        // given
        LocalDate targetDate = LocalDate.of(2024, 1, 15);
        transactionTemplate.execute(status -> {
            createOrdersForSeller(testSeller1, targetDate, 2);
            return null;
        });

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when - 첫 번째 실행
        JobExecution execution1 = jobLauncherTestUtils.launchJob(params);

        // then - 첫 번째 실행 성공
        assertThat(execution1.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        long countAfterFirst = settlementRepository.count();
        assertThat(countAfterFirst).isEqualTo(1);

        // when - 두 번째 실행 (동일 날짜)
        JobParameters params2 = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addLong("timestamp", System.currentTimeMillis() + 1000)  // 다른 timestamp
                .toJobParameters();
        JobExecution execution2 = jobLauncherTestUtils.launchJob(params2);

        // then - 두 번째 실행도 성공 (Skip 처리)
        assertThat(execution2.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 정산 데이터는 여전히 1개 (중복 생성 안됨)
        long countAfterSecond = settlementRepository.count();
        assertThat(countAfterSecond).isEqualTo(1);
    }

    @Test
    @DisplayName("주문 없는 판매자 - 정산 생성 안됨")
    void executeJob_NoOrders_NoSettlement() throws Exception {
        // given - 주문 없는 판매자
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 정산 데이터 생성 안됨
        long count = settlementRepository.count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("정산 금액 계산 정확성 검증")
    void executeJob_CalculationAccuracy() throws Exception {
        // given
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        // 주문 생성: 총 100,000원
        transactionTemplate.execute(status -> {
            createOrderWithAmount(testSeller1, targetDate, new BigDecimal("100000"));
            return null;
        });

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        transactionTemplate.execute(status -> {
            List<Settlement> settlements = settlementRepository.findAll();
            assertThat(settlements).hasSize(1);

            Settlement settlement = settlements.get(0);
            // 총 매출: 100,000
            assertThat(settlement.getGrossSalesAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));

            // 수수료 (10%): 10,000
            assertThat(settlement.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));

            // 부가세 (수수료의 10%): 1,000
            assertThat(settlement.getTaxAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

            // 정산액: 100,000 - 10,000 - 1,000 = 89,000
            assertThat(settlement.getPayoutAmount()).isEqualByComparingTo(new BigDecimal("89000.00"));
            return null;
        });
    }

    @Test
    @DisplayName("환불 포함 정산 계산")
    void executeJob_WithRefund() throws Exception {
        // given - refund.complete()가 LocalDateTime.now()를 사용하므로 오늘 날짜 사용
        LocalDate targetDate = LocalDate.now();

        // 주문 및 환불 생성
        transactionTemplate.execute(status -> {
            Order order = createOrderWithAmount(testSeller1, targetDate, new BigDecimal("100000"));
            OrderItem orderItem = order.getOrderItems().get(0);
            createRefund(orderItem, new BigDecimal("20000"));
            return null;
        });

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        transactionTemplate.execute(status -> {
            List<Settlement> settlements = settlementRepository.findAll();
            assertThat(settlements).hasSize(1);

            Settlement settlement = settlements.get(0);
            // 총 매출: 100,000
            assertThat(settlement.getGrossSalesAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));

            // 환불액: 20,000
            assertThat(settlement.getRefundAmount()).isEqualByComparingTo(new BigDecimal("20000.00"));

            // 순매출: 80,000
            // 수수료 (10%): 8,000
            // 부가세: 800
            // 정산액: 80,000 - 8,000 - 800 = 71,200
            assertThat(settlement.getPayoutAmount()).isEqualByComparingTo(new BigDecimal("71200.00"));
            return null;
        });
    }

    // ========== Helper Methods ==========

    private Seller createSeller(String sellerCode, String sellerName, SellerStatus status) {
        Seller seller = Seller.builder()
                .sellerCode(sellerCode)
                .sellerName(sellerName)
                .commissionRate(new BigDecimal("0.1000"))
                .status(status)
                .build();
        return sellerRepository.save(seller);
    }

    private void createOrdersForSeller(Seller seller, LocalDate targetDate, int count) {
        for (int i = 1; i <= count; i++) {
            createOrderWithAmount(seller, targetDate, new BigDecimal("50000"));
        }
    }

    private Order createOrderWithAmount(Seller seller, LocalDate targetDate, BigDecimal amount) {
        LocalDateTime orderDateTime = targetDate.atTime(10, 0);

        Order order = Order.builder()
                .orderNo("ORD-" + seller.getSellerCode() + "-" + System.nanoTime())
                .seller(seller)
                .orderStatus(OrderStatus.CONFIRMED)
                .orderDate(orderDateTime)
                .totalAmount(amount)
                .shippingFee(new BigDecimal("3000"))
                .build();

        OrderItem item = OrderItem.builder()
                .productName("테스트 상품")
                .unitPrice(amount)
                .quantity(1)
                .totalAmount(amount)
                .build();
        order.addOrderItem(item);

        Order savedOrder = orderRepository.save(order);

        // 결제 생성
        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod("CARD")
                .paymentAmount(amount.add(new BigDecimal("3000")))
                .paymentStatus(PaymentStatus.CONFIRMED)
                .build();
        paymentRepository.save(payment);

        return savedOrder;
    }

    private void createRefund(OrderItem orderItem, BigDecimal refundAmount) {
        Refund refund = Refund.builder()
                .orderItem(orderItem)
                .refundType(RefundType.PARTIAL_AMOUNT)
                .refundAmount(refundAmount)
                .refundQuantity(1)
                .refundReason("테스트 환불")
                .refundStatus(RefundStatus.APPROVED)
                .build();
        refund.complete();
        refundRepository.save(refund);
    }
}

