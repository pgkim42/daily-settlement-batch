package com.company.settlement.repository;

import com.company.settlement.domain.entity.Payment;
import com.company.settlement.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 주문별 결제 정보 조회
     * @param orderId 주문 ID
     * @return 결제 정보
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * 결제 상태별 결제 목록 조회
     * @param paymentStatus 결제 상태
     * @return 결제 목록
     */
    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * 특정 기간의 결제 목록 조회
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 결제 목록
     */
    @Query("SELECT p FROM Payment p WHERE p.paidAt BETWEEN :startDate AND :endDate")
    List<Payment> findByPaidAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 기간의 완료된 결제 목록 조회
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 결제 목록
     */
    @Query("SELECT p FROM Payment p WHERE p.paidAt BETWEEN :startDate AND :endDate " +
           "AND p.paymentStatus IN ('CONFIRMED', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED')")
    List<Payment> findCompletedPayments(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 판매자별 특정 기간의 결제 목록 조회
     * @param sellerId 판매자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 결제 목록
     */
    @Query("SELECT p FROM Payment p " +
           "JOIN p.order o " +
           "WHERE o.seller.id = :sellerId " +
           "AND p.paidAt BETWEEN :startDate AND :endDate " +
           "AND p.paymentStatus IN ('CONFIRMED', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED')")
    List<Payment> findCompletedPaymentsBySeller(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 판매자별 특정 기간의 총 결제 금액 집계
     * @param sellerId 판매자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 총 결제 금액
     */
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p " +
           "JOIN p.order o " +
           "WHERE o.seller.id = :sellerId " +
           "AND p.paidAt BETWEEN :startDate AND :endDate " +
           "AND p.paymentStatus IN ('CONFIRMED', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED')")
    BigDecimal calculateTotalPaymentAmount(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 결제 상태별 결제 수 조회
     * @param paymentStatus 결제 상태
     * @return 결제 수
     */
    long countByPaymentStatus(PaymentStatus paymentStatus);
}