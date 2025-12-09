package com.company.settlement.repository;

import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.SellerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 판매자 Repository
 */
@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {

    /**
     * 판매자 코드로 판매자 조회
     * @param sellerCode 판매자 코드
     * @return 판매자 정보
     */
    Optional<Seller> findBySellerCode(String sellerCode);

    /**
     * 판매자 코드와 상태로 판매자 조회
     * @param sellerCode 판매자 코드
     * @param status 판매자 상태
     * @return 판매자 정보
     */
    Optional<Seller> findBySellerCodeAndStatus(String sellerCode, SellerStatus status);

    /**
     * 특정 상태의 판매자 목록 조회
     * @param status 판매자 상태
     * @return 판매자 목록
     */
    List<Seller> findByStatus(SellerStatus status);

    /**
     * 정산 대상 판매자 목록 조회 (활성 상태)
     * @return 활성 상태 판매자 목록
     */
    @Query("SELECT s FROM Seller s WHERE s.status = :status")
    List<Seller> findSettlementEligibleSellers(@Param("status") SellerStatus status);

    /**
     * 판매자 코드 존재 여부 확인
     * @param sellerCode 판매자 코드
     * @return 존재 여부
     */
    boolean existsBySellerCode(String sellerCode);

    /**
     * 활성 판매자 수 조회
     * @return 활성 판매자 수
     */
    @Query("SELECT COUNT(s) FROM Seller s WHERE s.status = 'ACTIVE'")
    long countActiveSellers();
}