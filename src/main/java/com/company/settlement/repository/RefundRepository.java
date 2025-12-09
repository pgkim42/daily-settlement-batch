package com.company.settlement.repository;

import com.company.settlement.domain.entity.Refund;
import com.company.settlement.domain.enums.RefundStatus;
import com.company.settlement.domain.enums.RefundType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 환불 Repository
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * 주문 항목별 환불 목록 조회
     * @param orderItemId 주문 항목 ID
     * @return 환불 목록
     */
    List<Refund> findByOrderItemId(Long orderItemId);

    /**
     * 환불 상태별 환불 목록 조회
     * @param refundStatus 환불 상태
     * @return 환불 목록
     */
    List<Refund> findByRefundStatus(RefundStatus refundStatus);

    /**
     * 특정 기간의 완료된 환불 목록 조회
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 완료된 환불 목록
     */
    @Query("SELECT r FROM Refund r WHERE r.refundedAt BETWEEN :startDate AND :endDate " +
           "AND r.refundStatus = 'COMPLETED'")
    List<Refund> findCompletedRefunds(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 판매자별 특정 기간의 완료된 환불 목록 조회
     * @param sellerId 판매자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 완료된 환불 목록
     */
    @Query("SELECT r FROM Refund r " +
           "JOIN r.orderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.seller.id = :sellerId " +
           "AND r.refundedAt BETWEEN :startDate AND :endDate " +
           "AND r.refundStatus = 'COMPLETED'")
    List<Refund> findCompletedRefundsBySeller(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 판매자별 특정 기간의 총 환불 금액 집계
     * @param sellerId 판매자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 총 환불 금액
     */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r " +
           "JOIN r.orderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.seller.id = :sellerId " +
           "AND r.refundedAt BETWEEN :startDate AND :endDate " +
           "AND r.refundStatus = 'COMPLETED'")
    BigDecimal calculateTotalRefundAmount(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 환불 타입별 환불 목록 조회
     * @param refundType 환불 타입
     * @return 환불 목록
     */
    List<Refund> findByRefundType(RefundType refundType);

    /**
     * 특정 일자의 환불 현황 통계
     * @param date 일자
     * @return 통계 정보 배열 [전체 건수, 완료 건수, 전체 금액]
     */
    @Query("SELECT " +
           "COUNT(*) as totalCount, " +
           "SUM(CASE WHEN r.refundStatus = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount, " +
           "COALESCE(SUM(r.refundAmount), 0) as totalAmount " +
           "FROM Refund r " +
           "WHERE DATE(r.refundedAt) = :date")
    Object[] getRefundStatistics(@Param("date") LocalDate date);

    /**
     * 환불 상태별 환불 수 조회
     * @param refundStatus 환불 상태
     * @return 환불 수
     */
    long countByRefundStatus(RefundStatus refundStatus);
}