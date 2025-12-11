package com.company.settlement.batch.listener;

import com.company.settlement.batch.dto.SettlementContext;
import com.company.settlement.batch.exception.SettlementAlreadyExistsException;
import com.company.settlement.batch.exception.SettlementProcessingException;
import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.entity.Settlement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Skip 처리 Listener
 *
 * - 멱등성 체크로 인한 Skip 로깅
 * - 처리 오류로 인한 Skip 로깅
 * - Skip된 판매자 정보 수집
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementItemSkipListener implements SkipListener<Seller, SettlementContext> {

    private final JobExecutionListener jobExecutionListener;

    // Skip된 판매자 정보 저장 (모니터링/알림용)
    @Getter
    private final List<SkippedSellerInfo> skippedSellers =
        Collections.synchronizedList(new ArrayList<>());

    /**
     * Read 단계에서 Skip 발생 시
     */
    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[SkipListener] Skip in read phase: {}", t.getMessage());
    }

    /**
     * Process 단계에서 Skip 발생 시
     * - 멱등성 체크로 인한 Skip (SettlementAlreadyExistsException)
     * - 처리 오류로 인한 Skip (SettlementProcessingException)
     */
    @Override
    public void onSkipInProcess(Seller seller, Throwable t) {
        String reason;
        SkipReason skipReason;

        if (t instanceof SettlementAlreadyExistsException) {
            reason = "Settlement already exists";
            skipReason = SkipReason.ALREADY_EXISTS;
            log.info("[SkipListener] Skipped (already exists): seller={} ({})",
                     seller.getSellerName(), seller.getSellerCode());
        } else if (t instanceof SettlementProcessingException) {
            reason = t.getMessage();
            skipReason = SkipReason.PROCESSING_ERROR;
            log.error("[SkipListener] Skipped (processing error): seller={} ({}), error={}",
                      seller.getSellerName(), seller.getSellerCode(), reason);
        } else {
            reason = t.getMessage();
            skipReason = SkipReason.UNKNOWN;
            log.error("[SkipListener] Skipped (unknown): seller={} ({}), error={}",
                      seller.getSellerName(), seller.getSellerCode(), reason, t);
        }

        // Skip 정보 저장
        skippedSellers.add(new SkippedSellerInfo(
            seller.getId(),
            seller.getSellerCode(),
            seller.getSellerName(),
            skipReason,
            reason,
            LocalDateTime.now()
        ));

        // JobExecution 통계 업데이트 (이미 존재하는 경우는 실패가 아님)
        if (skipReason != SkipReason.ALREADY_EXISTS) {
            updateJobExecutionStats(reason);
        }
    }

    /**
     * Write 단계에서 Skip 발생 시
     */
    @Override
    public void onSkipInWrite(SettlementContext context, Throwable t) {
        Settlement settlement = context.settlement();
        log.error("[SkipListener] Skip in write phase: seller={}, error={}",
                  settlement.getSeller().getSellerCode(), t.getMessage());

        skippedSellers.add(new SkippedSellerInfo(
            settlement.getSeller().getId(),
            settlement.getSeller().getSellerCode(),
            settlement.getSeller().getSellerName(),
            SkipReason.WRITE_ERROR,
            t.getMessage(),
            LocalDateTime.now()
        ));

        updateJobExecutionStats(t.getMessage());
    }

    /**
     * JobExecution 통계 업데이트
     */
    private void updateJobExecutionStats(String reason) {
        var execution = jobExecutionListener.getCurrentExecution();
        if (execution != null) {
            execution.incrementFailureCount(reason);
        }
    }

    /**
     * Skip 정보 초기화
     */
    public void clear() {
        skippedSellers.clear();
    }

    /**
     * 에러로 인한 Skip 수 (이미 존재하는 것 제외)
     */
    public long getErrorSkipCount() {
        return skippedSellers.stream()
            .filter(info -> info.reason() != SkipReason.ALREADY_EXISTS)
            .count();
    }

    /**
     * Skip 사유
     */
    public enum SkipReason {
        ALREADY_EXISTS,      // 이미 정산 존재
        PROCESSING_ERROR,    // 처리 중 오류
        WRITE_ERROR,         // 저장 중 오류
        UNKNOWN              // 알 수 없는 오류
    }

    /**
     * Skip된 판매자 정보
     */
    public record SkippedSellerInfo(
        Long sellerId,
        String sellerCode,
        String sellerName,
        SkipReason reason,
        String errorMessage,
        LocalDateTime skippedAt
    ) {}
}
