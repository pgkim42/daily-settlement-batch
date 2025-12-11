package com.company.settlement.batch.job;

import com.company.settlement.batch.dto.SettlementContext;
import com.company.settlement.batch.exception.SettlementAlreadyExistsException;
import com.company.settlement.batch.exception.SettlementProcessingException;
import com.company.settlement.batch.listener.JobExecutionListener;
import com.company.settlement.batch.listener.SettlementItemSkipListener;
import com.company.settlement.batch.processor.SettlementProcessor;
import com.company.settlement.batch.writer.SettlementWriter;
import com.company.settlement.domain.entity.Seller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 일일 정산 배치 Job 설정
 *
 * Job: dailySettlementJob
 * Step: dailySettlementStep (chunk=100)
 *
 * 실행 방법:
 * java -jar app.jar --spring.batch.job.name=dailySettlementJob targetDate=2024-01-15
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DailySettlementJobConfig {

    public static final String JOB_NAME = "dailySettlementJob";
    public static final String STEP_NAME = "dailySettlementStep";
    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JpaPagingItemReader<Seller> sellerPagingItemReader;
    private final SettlementProcessor settlementProcessor;
    private final SettlementWriter settlementWriter;
    private final JobExecutionListener jobExecutionListener;
    private final SettlementItemSkipListener skipListener;

    /**
     * 일일 정산 Job 정의
     *
     * Job Parameters:
     * - targetDate: 정산 대상 날짜 (format: yyyy-MM-dd)
     *
     * 재시작 정책:
     * - RunIdIncrementer: 매 실행마다 새로운 run.id 부여 (중복 실행 방지)
     * - 재시작 허용: 실패한 Job 재시작 가능 (동일 파라미터로 FAILED 상태에서 재실행)
     * - 멱등성: SettlementProcessor에서 중복 정산 체크로 데이터 무결성 보장
     */
    @Bean
    public Job dailySettlementJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
            .incrementer(new RunIdIncrementer())
            // preventRestart() 제거: 실패한 Job 재시작 허용
            // 멱등성은 SettlementProcessor.checkIdempotency()에서 보장
            .listener(jobExecutionListener)
            .start(dailySettlementStep())
            .build();
    }

    /**
     * 정산 처리 Step 정의
     *
     * 처리 흐름:
     * 1. Reader: 활성 판매자 목록 조회
     * 2. Processor: 판매자별 정산 계산
     * 3. Writer: Settlement + SettlementItem 저장
     *
     * Fault Tolerant:
     * - SettlementAlreadyExistsException: Skip (이미 정산 존재)
     * - SettlementProcessingException: Skip (처리 오류)
     *
     * 재시작 정책:
     * - allowStartIfComplete(false): 완료된 Step 재실행 방지
     * - 멱등성 보장으로 재시작 시에도 안전
     */
    @Bean
    public Step dailySettlementStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<Seller, SettlementContext>chunk(CHUNK_SIZE, transactionManager)
            .reader(sellerPagingItemReader)
            .processor(settlementProcessor)
            .writer(settlementWriter)
            .faultTolerant()
            .skipLimit(Integer.MAX_VALUE)  // 무제한 Skip (판매자별 독립 처리)
            .skip(SettlementAlreadyExistsException.class)  // 이미 정산 존재 시 Skip
            .skip(SettlementProcessingException.class)     // 정산 처리 실패 시 Skip
            .listener(skipListener)
            .allowStartIfComplete(false)  // 완료된 Step 재실행 방지
            .build();
    }
}
