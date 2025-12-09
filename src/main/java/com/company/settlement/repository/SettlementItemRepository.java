package com.company.settlement.repository;

import com.company.settlement.domain.entity.SettlementItem;
import com.company.settlement.domain.enums.SettlementItemType;
import com.company.settlement.domain.enums.SettlementSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 정산 항목 Repository
 */
@Repository
public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {

    /**
     * 정산별 항목 목록 조회
     * @param settlementId 정산 ID
     * @return 정산 항목 목록
     */
    List<SettlementItem> findBySettlementId(Long settlementId);

    /**
     * 정산별 항목 타입별 목록 조회
     * @param settlementId 정산 ID
     * @param itemType 항목 타입
     * @return 정산 항목 목록
     */
    List<SettlementItem> findBySettlementIdAndItemType(Long settlementId, SettlementItemType itemType);

    /**
     * 원천 데이터로 정산 항목 조회
     * @param sourceType 원천 타입
     * @param sourceId 원천 ID
     * @return 정산 항목 목록
     */
    List<SettlementItem> findBySourceTypeAndSourceId(SettlementSource sourceType, Long sourceId);

    /**
     * 특정 정산의 판매 항목 목록 조회
     * @param settlementId 정산 ID
     * @return 판매 항목 목록
     */
    @Query("SELECT si FROM SettlementItem si " +
           "WHERE si.settlement.id = :settlementId " +
           "AND si.itemType = 'SALE'")
    List<SettlementItem> findSaleItems(@Param("settlementId") Long settlementId);

    /**
     * 특정 정산의 환불 항목 목록 조회
     * @param settlementId 정산 ID
     * @return 환불 항목 목록
     */
    @Query("SELECT si FROM SettlementItem si " +
           "WHERE si.settlement.id = :settlementId " +
           "AND si.itemType = 'REFUND'")
    List<SettlementItem> findRefundItems(@Param("settlementId") Long settlementId);

    /**
     * 특정 정산의 조정 항목 목록 조회
     * @param settlementId 정산 ID
     * @return 조정 항목 목록
     */
    @Query("SELECT si FROM SettlementItem si " +
           "WHERE si.settlement.id = :settlementId " +
           "AND si.itemType = 'ADJUSTMENT'")
    List<SettlementItem> findAdjustmentItems(@Param("settlementId") Long settlementId);

    /**
     * 특정 정산의 총 매출액 집계
     * @param settlementId 정산 ID
     * @return 총 매출액
     */
    @Query("SELECT COALESCE(SUM(si.grossAmount), 0) FROM SettlementItem si " +
           "WHERE si.settlement.id = :settlementId " +
           "AND si.itemType = 'SALE'")
    BigDecimal calculateTotalGrossSales(@Param("settlementId") Long settlementId);

    /**
     * 특정 정산의 총 환불액 집계
     * @param settlementId 정산 ID
     * @return 총 환불액
     */
    @Query("SELECT COALESCE(SUM(ABS(si.grossAmount)), 0) FROM SettlementItem si " +
           "WHERE si.settlement.id = :settlementId " +
           "AND si.itemType = 'REFUND'")
    BigDecimal calculateTotalRefundAmount(@Param("settlementId") Long settlementId);

    /**
     * 특정 정산의 총 수수료액 집계
     * @param settlementId 정산 ID
     * @return 총 수수료액
     */
    @Query("SELECT COALESCE(SUM(ABS(si.commissionAmount)), 0) FROM SettlementItem si " +
           "WHERE si.settlement.id = :settlementId")
    BigDecimal calculateTotalCommissionAmount(@Param("settlementId") Long settlementId);

    /**
     * 원천 주문 항목 ID들의 정산 항목 존재 여부 확인
     * @param orderItemIds 주문 항목 ID 목록
     * @return 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(si) > 0 THEN true ELSE false END " +
           "FROM SettlementItem si " +
           "WHERE si.sourceType = 'ORDER_ITEM' " +
           "AND si.sourceId IN :orderItemIds")
    boolean existsByOrderItemIds(@Param("orderItemIds") List<Long> orderItemIds);

    /**
     * 원천 환불 ID들의 정산 항목 존재 여부 확인
     * @param refundIds 환불 ID 목록
     * @return 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(si) > 0 THEN true ELSE false END " +
           "FROM SettlementItem si " +
           "WHERE si.sourceType = 'REFUND' " +
           "AND si.sourceId IN :refundIds")
    boolean existsByRefundIds(@Param("refundIds") List<Long> refundIds);

    /**
     * 특정 정산의 항목 수 조회
     * @param settlementId 정산 ID
     * @param itemType 항목 타입
     * @return 항목 수
     */
    long countBySettlementIdAndItemType(Long settlementId, SettlementItemType itemType);
}