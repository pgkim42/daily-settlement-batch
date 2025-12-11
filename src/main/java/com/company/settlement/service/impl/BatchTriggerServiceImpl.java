package com.company.settlement.service.impl;

import com.company.settlement.batch.job.DailySettlementJobConfig;
import com.company.settlement.dto.response.BatchTriggerResponse;
import com.company.settlement.exception.BatchAlreadyRunningException;
import com.company.settlement.service.BatchTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 배치 수동 실행 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchTriggerServiceImpl implements BatchTriggerService {

    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;
    private final JobExplorer jobExplorer;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public BatchTriggerResponse triggerSettlementBatch(LocalDate targetDate) {
        log.info("Manual batch trigger requested for date: {}", targetDate);

        // 실행 중인 Job이 있는지 확인
        if (isJobRunning()) {
            throw new BatchAlreadyRunningException(DailySettlementJobConfig.JOB_NAME);
        }

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", targetDate.format(DATE_FORMATTER))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            JobExecution execution = jobLauncher.run(dailySettlementJob, jobParameters);

            log.info("Manual batch started. JobExecutionId: {}, Status: {}",
                     execution.getId(), execution.getStatus());

            return BatchTriggerResponse.success(
                execution.getId(),
                DailySettlementJobConfig.JOB_NAME,
                targetDate,
                execution.getStartTime()
            );
        } catch (Exception e) {
            log.error("Failed to trigger manual batch for date: {}", targetDate, e);
            return BatchTriggerResponse.failure(
                DailySettlementJobConfig.JOB_NAME,
                targetDate,
                e.getMessage()
            );
        }
    }

    /**
     * 현재 실행 중인 Job이 있는지 확인
     */
    private boolean isJobRunning() {
        var runningExecutions = jobExplorer.findRunningJobExecutions(DailySettlementJobConfig.JOB_NAME);
        return !runningExecutions.isEmpty();
    }
}
