package com.company.settlement.service;

import com.company.settlement.dto.response.SettlementDetailResponse;
import com.company.settlement.dto.response.SettlementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 정산 서비스 (판매자용)
 */
public interface SettlementService {

    /**
     * 내 정산 목록 조회
     *
     * @param sellerId 판매자 ID
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    Page<SettlementResponse> getMySettlements(Long sellerId, Pageable pageable);

    /**
     * 정산 상세 조회
     *
     * @param sellerId 판매자 ID
     * @param settlementId 정산 ID
     * @return 정산 상세 정보
     */
    SettlementDetailResponse getSettlementDetail(Long sellerId, Long settlementId);
}
