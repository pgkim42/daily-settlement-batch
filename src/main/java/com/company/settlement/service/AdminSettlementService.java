package com.company.settlement.service;

import com.company.settlement.dto.response.SettlementDetailResponse;
import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.dto.response.SettlementStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * 관리자용 정산 서비스
 */
public interface AdminSettlementService {

    /**
     * 전체 정산 목록 조회
     *
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    Page<SettlementResponse> getAllSettlements(Pageable pageable);

    /**
     * 기간별 정산 목록 조회
     *
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    Page<SettlementResponse> getSettlementsByPeriod(LocalDate periodStart, LocalDate periodEnd, Pageable pageable);

    /**
     * 정산 상세 조회 (관리자용 - 권한 체크 없음)
     *
     * @param settlementId 정산 ID
     * @return 정산 상세 정보
     */
    SettlementDetailResponse getSettlementDetail(Long settlementId);

    /**
     * 기간별 정산 통계 조회
     *
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @return 정산 통계
     */
    SettlementStatisticsResponse getStatistics(LocalDate periodStart, LocalDate periodEnd);
}
