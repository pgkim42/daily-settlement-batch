package com.company.settlement.batch.scheduler;

import com.company.settlement.batch.job.DailySettlementJobConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 일일 정산 스케줄러
 *
 * 매일 새벽 2시(KST)에 전일 정산 배치를 실행합니다.
 * settlement.scheduler.enabled=true 설정 시에만 활성화됩니다.
 */
@Component
@ConditionalOnProperty(name = "settlement.scheduler.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 매일 새벽 2시(KST)에 전일 정산 배치 실행
     *
     * Cron: 0 0 2 * * ? (매일 02:00:00)
     * Zone: Asia/Seoul (KST)
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Seoul")
    public void runDailySettlement() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        log.info("[Scheduler] Starting daily settlement batch for date: {}", targetDate);

        try {
            JobParameters jobParameters = createJobParameters(targetDate);
            var execution = jobLauncher.run(dailySettlementJob, jobParameters);

            log.info("[Scheduler] Daily settlement batch completed. JobExecutionId: {}, Status: {}",
                     execution.getId(), execution.getStatus());
        } catch (Exception e) {
            log.error("[Scheduler] Failed to execute daily settlement batch for date: {}",
                      targetDate, e);
        }
    }

    /**
     * 특정 날짜의 정산 배치를 수동으로 실행
     *
     * @param targetDate 정산 대상 날짜
     */
    public void runSettlementForDate(LocalDate targetDate) {
        log.info("[Scheduler] Manual trigger for date: {}", targetDate);

        try {
            JobParameters jobParameters = createJobParameters(targetDate);
            var execution = jobLauncher.run(dailySettlementJob, jobParameters);

            log.info("[Scheduler] Manual settlement batch completed. JobExecutionId: {}, Status: {}",
                     execution.getId(), execution.getStatus());
        } catch (Exception e) {
            log.error("[Scheduler] Failed to execute manual settlement batch for date: {}",
                      targetDate, e);
            throw new RuntimeException("Failed to execute settlement batch", e);
        }
    }

    /**
     * JobParameters 생성
     *
     * @param targetDate 정산 대상 날짜
     * @return JobParameters
     */
    private JobParameters createJobParameters(LocalDate targetDate) {
        return new JobParametersBuilder()
            .addString("targetDate", targetDate.format(DATE_FORMATTER))
            .addLong("timestamp", System.currentTimeMillis())  // 재실행 허용을 위한 타임스탬프
            .toJobParameters();
    }
}
