package com.company.settlement.controller;

import com.company.settlement.dto.request.BatchTriggerRequest;
import com.company.settlement.dto.response.BatchTriggerResponse;
import com.company.settlement.dto.response.SettlementDetailResponse;
import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.dto.response.SettlementStatisticsResponse;
import com.company.settlement.service.AdminSettlementService;
import com.company.settlement.service.BatchTriggerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 관리자용 정산 API 컨트롤러
 *
 * 관리자가 전체 정산 현황을 조회하고 배치를 수동 실행할 수 있는 API
 */
@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
@Slf4j
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;
    private final BatchTriggerService batchTriggerService;

    /**
     * 전체 정산 목록 조회
     *
     * GET /api/admin/settlements?page=0&size=20
     *
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    @GetMapping
    public ResponseEntity<Page<SettlementResponse>> getAllSettlements(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("GET /api/admin/settlements - pageable: {}", pageable);

        Page<SettlementResponse> settlements = adminSettlementService.getAllSettlements(pageable);

        return ResponseEntity.ok(settlements);
    }

    /**
     * 기간별 정산 목록 조회
     *
     * GET /api/admin/settlements/period?start=2024-01-01&end=2024-01-31
     *
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @param pageable 페이징 정보
     * @return 정산 목록
     */
    @GetMapping("/period")
    public ResponseEntity<Page<SettlementResponse>> getSettlementsByPeriod(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("GET /api/admin/settlements/period - period: {} ~ {}, pageable: {}",
                 periodStart, periodEnd, pageable);

        Page<SettlementResponse> settlements = adminSettlementService
            .getSettlementsByPeriod(periodStart, periodEnd, pageable);

        return ResponseEntity.ok(settlements);
    }

    /**
     * 정산 상세 조회 (관리자용)
     *
     * GET /api/admin/settlements/{settlementId}
     *
     * @param settlementId 정산 ID
     * @return 정산 상세 정보
     */
    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementDetailResponse> getSettlementDetail(
            @PathVariable Long settlementId) {

        log.info("GET /api/admin/settlements/{}", settlementId);

        SettlementDetailResponse detail = adminSettlementService.getSettlementDetail(settlementId);

        return ResponseEntity.ok(detail);
    }

    /**
     * 기간별 정산 통계 조회
     *
     * GET /api/admin/settlements/statistics?start=2024-01-01&end=2024-01-31
     *
     * @param periodStart 기간 시작일
     * @param periodEnd 기간 종료일
     * @return 정산 통계
     */
    @GetMapping("/statistics")
    public ResponseEntity<SettlementStatisticsResponse> getStatistics(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {

        log.info("GET /api/admin/settlements/statistics - period: {} ~ {}", periodStart, periodEnd);

        SettlementStatisticsResponse statistics = adminSettlementService.getStatistics(periodStart, periodEnd);

        return ResponseEntity.ok(statistics);
    }

    /**
     * 배치 수동 실행
     *
     * POST /api/admin/settlements/batch/trigger
     *
     * @param request 배치 실행 요청 (targetDate)
     * @return 실행 결과
     */
    @PostMapping("/batch/trigger")
    public ResponseEntity<BatchTriggerResponse> triggerBatch(
            @Valid @RequestBody BatchTriggerRequest request) {

        log.info("POST /api/admin/settlements/batch/trigger - targetDate: {}", request.targetDate());

        BatchTriggerResponse response = batchTriggerService.triggerSettlementBatch(request.targetDate());

        return ResponseEntity.ok(response);
    }
}
