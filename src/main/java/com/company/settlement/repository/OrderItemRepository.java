package com.company.settlement.repository;

import com.company.settlement.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 항목 Repository
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 주문별 주문 항목 목록 조회
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 환불되지 않은 주문 항목 목록 조회
     * @param orderId 주문 ID
     * @return 환불되지 않은 주문 항목 목록
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.isRefunded = false")
    List<OrderItem> findByOrderIdAndIsRefundedFalse(@Param("orderId") Long orderId);

    /**
     * 판매자별 특정 기간의 주문 항목 목록 조회
     * @param sellerId 판매자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 주문 항목 목록
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.seller.id = :sellerId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND oi.isRefunded = false")
    List<OrderItem> findItemsForSettlement(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 판매자별 특정 기간의 총 판매 금액 집계
     * @param sellerId 판매자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 총 판매 금액
     */
    @Query("SELECT COALESCE(SUM(oi.totalAmount), 0) FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.seller.id = :sellerId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND oi.isRefunded = false")
    BigDecimal calculateTotalSalesAmount(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 환불된 주문 항목 ID 목록 조회
     * @param orderIds 주문 ID 목록
     * @return 환불된 주문 항목 ID 목록
     */
    @Query("SELECT oi.id FROM OrderItem oi WHERE oi.order.id IN :orderIds AND oi.isRefunded = true")
    List<Long> findRefundedItemIds(@Param("orderIds") List<Long> orderIds);

    /**
     * 환불 가능한 주문 항목 수 조회
     * @param orderId 주문 ID
     * @return 환불 가능 수량
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.order.id = :orderId AND oi.isRefunded = false")
    Integer countRefundableQuantity(@Param("orderId") Long orderId);
}