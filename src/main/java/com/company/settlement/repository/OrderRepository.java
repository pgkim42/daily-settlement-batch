package com.company.settlement.repository;

import com.company.settlement.domain.entity.Order;
import com.company.settlement.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문번호로 주문 조회
     * @param orderNo 주문번호
     * @return 주문 정보
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 판매자별 주문 목록 조회
     * @param sellerId 판매자 ID
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    /**
     * 특정 기간의 판매자별 주문 목록 조회
     * @param sellerId 판매자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 주문 목록
     */
    @Query("SELECT o FROM Order o WHERE o.seller.id = :sellerId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findBySellerIdAndOrderDateBetween(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 기간의 정산 대상 주문 목록 조회
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @param orderStatuses 주문 상태 목록
     * @return 주문 목록
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.orderStatus IN :orderStatuses")
    List<Order> findByOrderDateBetweenAndOrderStatusIn(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderStatuses") List<OrderStatus> orderStatuses);

    /**
     * 판매자별 특정 기간의 정산 대상 주문 목록 조회
     * @param sellerId 판매자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 주문 목록
     */
    @Query("SELECT o FROM Order o WHERE o.seller.id = :sellerId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.orderStatus IN ('CONFIRMED', 'SHIPPED', 'DELIVERED')")
    List<Order> findSettlementTargetOrders(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 주문 상태별 주문 수 조회
     * @param orderStatus 주문 상태
     * @return 주문 수
     */
    long countByOrderStatus(OrderStatus orderStatus);

    /**
     * 판매자별 주문 수 조회
     * @param sellerId 판매자 ID
     * @return 주문 수
     */
    long countBySellerId(Long sellerId);
}