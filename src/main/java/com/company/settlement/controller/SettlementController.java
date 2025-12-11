package com.company.settlement.controller;

import com.company.settlement.dto.response.SettlementDetailResponse;
import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 정산 API 컨트롤러 (판매자용)
 *
 * 판매자가 자신의 정산 내역을 조회할 수 있는 API
 */
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 내 정산 목록 조회
     *
     * GET /api/settlements?page=0&size=20&sort=createdAt,desc
     *
     * @param sellerId 판매자 ID (헤더에서 추출 - 실제로는 인증 정보에서)
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    @GetMapping
    public ResponseEntity<Page<SettlementResponse>> getMySettlements(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("GET /api/settlements - sellerId: {}, pageable: {}", sellerId, pageable);

        Page<SettlementResponse> settlements = settlementService.getMySettlements(sellerId, pageable);

        return ResponseEntity.ok(settlements);
    }

    /**
     * 정산 상세 조회
     *
     * GET /api/settlements/{settlementId}
     *
     * @param sellerId 판매자 ID
     * @param settlementId 정산 ID
     * @return 정산 상세 정보
     */
    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementDetailResponse> getSettlementDetail(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long settlementId) {

        log.info("GET /api/settlements/{} - sellerId: {}", settlementId, sellerId);

        SettlementDetailResponse detail = settlementService.getSettlementDetail(sellerId, settlementId);

        return ResponseEntity.ok(detail);
    }
}
