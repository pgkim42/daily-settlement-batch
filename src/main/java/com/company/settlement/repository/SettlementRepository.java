package com.company.settlement.repository;

import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.domain.enums.CycleType;
import com.company.settlement.domain.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 정산 Repository
 */
@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /**
     * 판매자별 정산 목록 조회
     * @param sellerId 판매자 ID
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    Page<Settlement> findBySellerId(Long sellerId, Pageable pageable);

    /**
     * 판매자와 기간으로 정산 조회 (멱등성 체크용)
     * @param sellerId 판매자 ID
     * @param cycleType 정산 주기
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @return 정산 정보
     */
    Optional<Settlement> findBySellerIdAndCycleTypeAndPeriodStartAndPeriodEnd(
            Long sellerId, CycleType cycleType, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 판매자와 기간으로 활성 상태 정산 조회
     * @param sellerId 판매자 ID
     * @param cycleType 정산 주기
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @return 정산 정보
     */
    @Query("SELECT s FROM Settlement s WHERE s.seller.id = :sellerId " +
           "AND s.cycleType = :cycleType " +
           "AND s.periodStart = :periodStart " +
           "AND s.periodEnd = :periodEnd " +
           "AND s.status = 'CONFIRMED'")
    Optional<Settlement> findActiveSettlement(
            @Param("sellerId") Long sellerId,
            @Param("cycleType") CycleType cycleType,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    /**
     * 특정 기간의 정산 목록 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @param status 정산 상태
     * @return 정산 목록
     */
    @Query("SELECT s FROM Settlement s " +
           "WHERE s.periodStart BETWEEN :startDate AND :endDate " +
           "AND (:status IS NULL OR s.status = :status)")
    List<Settlement> findByPeriodStartBetweenAndStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") SettlementStatus status);

    /**
     * 판매자별 특정 기간의 마지막 정산 조회
     * @param sellerId 판매자 ID
     * @param periodEnd 기간 종료일
     * @return 마지막 정산
     */
    @Query("SELECT s FROM Settlement s " +
           "WHERE s.seller.id = :sellerId " +
           "AND s.periodEnd = :periodEnd " +
           "ORDER BY s.createdAt DESC " +
           "LIMIT 1")
    Optional<Settlement> findLastSettlementBySellerAndPeriod(
            @Param("sellerId") Long sellerId,
            @Param("periodEnd") LocalDate periodEnd);

    /**
     * 정산 상태별 정산 목록 조회
     * @param status 정산 상태
     * @return 정산 목록
     */
    List<Settlement> findByStatus(SettlementStatus status);

    /**
     * 지급 대상 정산 목록 조회 (확정 상태)
     * @param limit 조회할 건수
     * @return 지급 대상 정산 목록
     */
    @Query("SELECT s FROM Settlement s " +
           "WHERE s.status = 'CONFIRMED' " +
           "AND s.paidAt IS NULL " +
           "ORDER BY s.confirmedAt ASC")
    List<Settlement> findPendingPayments(@Param("limit") int limit);

    /**
     * 판매자별 총 정산 금액 집계
     * @param sellerId 판매자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 총 정산 금액
     */
    @Query("SELECT COALESCE(SUM(s.payoutAmount), 0) FROM Settlement s " +
           "WHERE s.seller.id = :sellerId " +
           "AND s.periodStart BETWEEN :startDate AND :endDate " +
           "AND s.status = 'PAID'")
    BigDecimal calculateTotalPayoutAmount(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 일자의 정산 현황 통계
     * @param date 일자
     * @return 통계 정보 배열 [전체 건수, 확정 건수, 지급 건수, 총 지급액]
     */
    @Query("SELECT " +
           "COUNT(*) as totalCount, " +
           "SUM(CASE WHEN s.status = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmedCount, " +
           "SUM(CASE WHEN s.status = 'PAID' THEN 1 ELSE 0 END) as paidCount, " +
           "COALESCE(SUM(s.payoutAmount), 0) as totalPayout " +
           "FROM Settlement s " +
           "WHERE DATE(s.createdAt) = :date")
    Object[] getSettlementStatistics(@Param("date") LocalDate date);

    /**
     * 낙관적 잠금을 통한 정산 조회
     * @param settlementId 정산 ID
     * @return 정산 정보
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Settlement s WHERE s.id = :settlementId")
    Optional<Settlement> findByIdWithLock(@Param("settlementId") Long settlementId);

    /**
     * 정산 상태별 건수 조회
     * @param status 정산 상태
     * @return 건수
     */
    long countByStatus(SettlementStatus status);

    /**
     * 취소된 정산 목록 조회
     * @param limit 조회할 건수
     * @return 취소된 정산 목록
     */
    @Query("SELECT s FROM Settlement s " +
           "WHERE s.status = 'CANCELLED' " +
           "ORDER BY s.cancelledAt DESC " +
           "LIMIT :limit")
    List<Settlement> findCancelledSettlements(@Param("limit") int limit);

    // ========== API용 쿼리 메서드 ==========

    /**
     * 정산 상세 조회 (Seller Fetch Join)
     * @param settlementId 정산 ID
     * @return 정산 정보 (Seller 포함)
     */
    @Query("SELECT s FROM Settlement s " +
           "JOIN FETCH s.seller " +
           "WHERE s.id = :settlementId")
    Optional<Settlement> findByIdWithSeller(@Param("settlementId") Long settlementId);

    /**
     * 정산 상세 조회 (Seller + Items Fetch Join)
     * @param settlementId 정산 ID
     * @return 정산 정보 (Seller, Items 포함)
     */
    @Query("SELECT DISTINCT s FROM Settlement s " +
           "JOIN FETCH s.seller " +
           "LEFT JOIN FETCH s.settlementItems " +
           "WHERE s.id = :settlementId")
    Optional<Settlement> findByIdWithSellerAndItems(@Param("settlementId") Long settlementId);

    /**
     * 판매자별 정산 목록 조회 (Seller Fetch Join)
     * @param sellerId 판매자 ID
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    @Query(value = "SELECT s FROM Settlement s JOIN FETCH s.seller WHERE s.seller.id = :sellerId",
           countQuery = "SELECT COUNT(s) FROM Settlement s WHERE s.seller.id = :sellerId")
    Page<Settlement> findBySellerIdWithSeller(@Param("sellerId") Long sellerId, Pageable pageable);

    /**
     * 전체 정산 목록 조회 (관리자용, Seller Fetch Join)
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    @Query(value = "SELECT s FROM Settlement s JOIN FETCH s.seller",
           countQuery = "SELECT COUNT(s) FROM Settlement s")
    Page<Settlement> findAllWithSeller(Pageable pageable);

    /**
     * 기간별 정산 목록 조회 (관리자용, Seller Fetch Join)
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    @Query(value = "SELECT s FROM Settlement s JOIN FETCH s.seller " +
           "WHERE s.periodStart >= :periodStart AND s.periodEnd <= :periodEnd",
           countQuery = "SELECT COUNT(s) FROM Settlement s " +
           "WHERE s.periodStart >= :periodStart AND s.periodEnd <= :periodEnd")
    Page<Settlement> findByPeriodWithSeller(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            Pageable pageable);

    /**
     * 특정 기간의 정산 통계 (금액 기준)
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @return 통계 정보 [총매출액, 총환불액, 총수수료, 총부가세, 총정산액]
     */
    @Query("SELECT " +
           "COALESCE(SUM(s.totalSalesAmount), 0), " +
           "COALESCE(SUM(s.totalRefundAmount), 0), " +
           "COALESCE(SUM(s.commissionAmount), 0), " +
           "COALESCE(SUM(s.vatAmount), 0), " +
           "COALESCE(SUM(s.settlementAmount), 0) " +
           "FROM Settlement s " +
           "WHERE s.periodStart >= :periodStart AND s.periodEnd <= :periodEnd")
    Object[] getSettlementAmountStatistics(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    /**
     * 특정 기간 및 상태별 정산 건수 조회
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @param status 정산 상태
     * @return 건수
     */
    @Query("SELECT COUNT(s) FROM Settlement s " +
           "WHERE s.periodStart >= :periodStart AND s.periodEnd <= :periodEnd " +
           "AND s.status = :status")
    long countByPeriodAndStatus(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("status") SettlementStatus status);
}