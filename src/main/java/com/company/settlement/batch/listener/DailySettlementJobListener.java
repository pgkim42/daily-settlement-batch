package com.company.settlement.batch.listener;

import com.company.settlement.domain.entity.SettlementJobExecution;
import com.company.settlement.domain.enums.SettlementJobStatus;
import com.company.settlement.repository.SellerRepository;
import com.company.settlement.repository.SettlementJobExecutionRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Job 실행 이력 관리 Listener
 *
 * - Job 시작/종료 시 SettlementJobExecution 엔티티 관리
 * - 중복 실행 체크
 * - 실행 통계 집계
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailySettlementJobListener implements org.springframework.batch.core.JobExecutionListener {

    public static final String JOB_NAME = "dailySettlementJob";

    private final SettlementJobExecutionRepository jobExecutionRepository;
    private final SellerRepository sellerRepository;

    @Getter
    private SettlementJobExecution currentExecution;

    /**
     * Job 시작 전 실행
     * - 중복 실행 체크
     * - 실행 이력 생성
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        String targetDateStr = jobExecution.getJobParameters().getString("targetDate");
        LocalDate targetDate = LocalDate.parse(targetDateStr);

        log.info("[DailySettlementJobListener] Starting job: {}, targetDate: {}", JOB_NAME, targetDate);

        // 중복 실행 체크
        Optional<SettlementJobExecution> existingExecution =
            jobExecutionRepository.findByJobNameAndExecutionDate(JOB_NAME, targetDate);

        if (existingExecution.isPresent()) {
            SettlementJobExecution existing = existingExecution.get();
            if (existing.getExecutionStatus() == SettlementJobStatus.COMPLETED) {
                log.warn("[DailySettlementJobListener] Job already completed for date: {}", targetDate);
                // ExecutionContext에 스킵 플래그 설정
                jobExecution.getExecutionContext().put("skipExecution", true);
                return;
            }
            // 이전 실패/부분 실패 실행이 있으면 재실행 허용
            log.info("[DailySettlementJobListener] Re-running job for date: {} (previous status: {})",
                     targetDate, existing.getExecutionStatus());
            // 기존 실행 이력 삭제 후 새로 생성
            jobExecutionRepository.delete(existing);
        }

        // 정산 대상 판매자 수 조회
        long totalSellers = sellerRepository.countActiveSellers();

        // 실행 이력 생성
        currentExecution = SettlementJobExecution.builder()
            .jobName(JOB_NAME)
            .executionDate(targetDate)
            .executionStatus(SettlementJobStatus.STARTED)
            .totalSellers((int) totalSellers)
            .build();

        currentExecution = jobExecutionRepository.save(currentExecution);

        // ExecutionContext에 실행 ID 저장 (다른 Listener에서 참조용)
        jobExecution.getExecutionContext().putLong("jobExecutionId", currentExecution.getId());

        log.info("[DailySettlementJobListener] Created job execution record: id={}, totalSellers={}",
                 currentExecution.getId(), totalSellers);
    }

    /**
     * Job 완료 후 실행
     * - 실행 이력 업데이트
     * - 성공/실패 카운트 집계
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (currentExecution == null) {
            log.warn("[DailySettlementJobListener] No execution record found");
            return;
        }

        BatchStatus batchStatus = jobExecution.getStatus();

        // StepExecution에서 처리 통계 가져오기
        int writeCount = 0;
        int skipCount = 0;

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            writeCount += stepExecution.getWriteCount();
            skipCount += stepExecution.getSkipCount();
        }

        // 실행 이력 업데이트
        if (batchStatus == BatchStatus.COMPLETED) {
            currentExecution.complete(writeCount, skipCount);
            log.info("[DailySettlementJobListener] Job completed: success={}, skip={}",
                     writeCount, skipCount);
        } else if (batchStatus == BatchStatus.FAILED) {
            String errorMessage = extractErrorMessage(jobExecution);
            currentExecution.fail(errorMessage);
            log.error("[DailySettlementJobListener] Job failed: {}", errorMessage);
        } else {
            // STOPPED, ABANDONED 등 기타 상태
            currentExecution.complete(writeCount, skipCount);
            log.warn("[DailySettlementJobListener] Job ended with status: {}", batchStatus);
        }

        jobExecutionRepository.save(currentExecution);

        log.info("[DailySettlementJobListener] Job execution completed: id={}, status={}, " +
                 "successRate={}%, duration={}s",
                 currentExecution.getId(),
                 currentExecution.getExecutionStatus(),
                 String.format("%.2f", currentExecution.getSuccessRate()),
                 currentExecution.getExecutionTimeInSeconds());
    }

    /**
     * 에러 메시지 추출
     */
    private String extractErrorMessage(JobExecution jobExecution) {
        StringBuilder sb = new StringBuilder();

        for (Throwable exception : jobExecution.getAllFailureExceptions()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(exception.getMessage());
        }

        return sb.length() > 0 ? sb.toString() : "Unknown error";
    }
}
