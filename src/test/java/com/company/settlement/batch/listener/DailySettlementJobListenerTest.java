package com.company.settlement.batch.listener;

import com.company.settlement.domain.entity.SettlementJobExecution;
import com.company.settlement.domain.enums.SettlementJobStatus;
import com.company.settlement.repository.SellerRepository;
import com.company.settlement.repository.SettlementJobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DailySettlementJobListener 단위 테스트
 *
 * 검증 항목:
 * - Job 시작 전 실행 이력 생성
 * - 중복 실행 체크 (이미 완료된 Job은 스킵)
 * - Job 완료 후 실행 이력 업데이트
 * - 성공/실패 카운트 집계
 * - 실패 시 재실행 허용
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailySettlementJobListener 단위 테스트")
class DailySettlementJobListenerTest {

    @Mock
    private SettlementJobExecutionRepository jobExecutionRepository;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private DailySettlementJobListener listener;

    private JobExecution jobExecution;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        targetDate = LocalDate.of(2025, 1, 15);
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("targetDate", targetDate.toString())
            .toJobParameters();

        jobExecution = new JobExecution(1L, jobParameters);
    }

    @Nested
    @DisplayName("beforeJob: Job 시작 전 처리")
    class BeforeJobTests {

        @Test
        @DisplayName("새로운 Job 실행 이력을 생성해야 한다")
        void createNewJobExecution() {
            // Given
            when(jobExecutionRepository.findByJobNameAndExecutionDate(
                eq(DailySettlementJobListener.JOB_NAME), eq(targetDate)
            )).thenReturn(Optional.empty());
            when(sellerRepository.countActiveSellers()).thenReturn(10L);
            when(jobExecutionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution exec = invocation.getArgument(0);
                    // ID를 설정하여 반환 (리플렉션)
                    setId(exec, 1L);
                    return exec;
                });

            // When
            listener.beforeJob(jobExecution);

            // Then
            assertThat(listener.getCurrentExecution()).isNotNull();
            assertThat(listener.getCurrentExecution().getExecutionStatus()).isEqualTo(SettlementJobStatus.STARTED);
            assertThat(listener.getCurrentExecution().getTotalSellers()).isEqualTo(10);

            verify(jobExecutionRepository).save(any(SettlementJobExecution.class));
        }

        @Test
        @DisplayName("ExecutionContext에 실행 ID를 저장해야 한다")
        void saveExecutionIdToExecutionContext() {
            // Given
            when(jobExecutionRepository.findByJobNameAndExecutionDate(any(), any()))
                .thenReturn(Optional.empty());
            when(sellerRepository.countActiveSellers()).thenReturn(5L);
            when(jobExecutionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution exec = invocation.getArgument(0);
                    setId(exec, 1L);
                    return exec;
                });

            // When
            listener.beforeJob(jobExecution);

            // Then
            Long executionId = jobExecution.getExecutionContext().getLong("jobExecutionId");
            assertThat(executionId).isNotNull();
            assertThat(executionId).isEqualTo(listener.getCurrentExecution().getId());
        }

        @Test
        @DisplayName("이미 완료된 Job이 있으면 skipExecution 플래그를 설정해야 한다")
        void setSkipFlagWhenJobAlreadyCompleted() {
            // Given
            SettlementJobExecution existingExecution = SettlementJobExecution.builder()
                .jobName(DailySettlementJobListener.JOB_NAME)
                .executionDate(targetDate)
                .executionStatus(SettlementJobStatus.COMPLETED)
                .totalSellers(10)
                .build();

            when(jobExecutionRepository.findByJobNameAndExecutionDate(
                eq(DailySettlementJobListener.JOB_NAME), eq(targetDate)
            )).thenReturn(Optional.of(existingExecution));

            // When
            listener.beforeJob(jobExecution);

            // Then - ExecutionContext의 put은 boolean을 Boolean으로 저장
            Object skipExecution = jobExecution.getExecutionContext().get("skipExecution");
            assertThat(skipExecution).isNotNull();
            assertThat(skipExecution).isEqualTo(true);  // Boolean 객체

            // 새로운 실행 이력은 생성하지 않음
            verify(jobExecutionRepository, never()).save(any(SettlementJobExecution.class));
        }

        @Test
        @DisplayName("이전 실패 Job이 있으면 재실행을 허용해야 한다")
        void allowRetryWhenPreviousJobFailed() {
            // Given
            SettlementJobExecution failedExecution = SettlementJobExecution.builder()
                .jobName(DailySettlementJobListener.JOB_NAME)
                .executionDate(targetDate)
                .executionStatus(SettlementJobStatus.FAILED)
                .totalSellers(10)
                .build();

            when(jobExecutionRepository.findByJobNameAndExecutionDate(
                eq(DailySettlementJobListener.JOB_NAME), eq(targetDate)
            )).thenReturn(Optional.of(failedExecution));
            when(sellerRepository.countActiveSellers()).thenReturn(10L);
            when(jobExecutionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution exec = invocation.getArgument(0);
                    setId(exec, 2L);
                    return exec;
                });

            // When
            listener.beforeJob(jobExecution);

            // Then
            assertThat(listener.getCurrentExecution()).isNotNull();
            assertThat(listener.getCurrentExecution().getExecutionStatus()).isEqualTo(SettlementJobStatus.STARTED);

            // 기존 실패 실행은 삭제되고 새로운 실행이 생성됨
            verify(jobExecutionRepository).delete(failedExecution);
            verify(jobExecutionRepository).save(any(SettlementJobExecution.class));
        }
    }

    @Nested
    @DisplayName("afterJob: Job 완료 후 처리")
    class AfterJobTests {

        @Test
        @DisplayName("COMPLETED 상태로 실행 이력을 업데이트해야 한다")
        void updateExecutionOnCompletion() {
            // Given
            when(jobExecutionRepository.findByJobNameAndExecutionDate(any(), any()))
                .thenReturn(Optional.empty());
            when(sellerRepository.countActiveSellers()).thenReturn(10L);
            when(jobExecutionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution exec = invocation.getArgument(0);
                    setId(exec, 1L);
                    return exec;
                });

            listener.beforeJob(jobExecution);

            // Job 완료 상태 설정 및 StepExecution 추가
            jobExecution.setStatus(BatchStatus.COMPLETED);
            StepExecution stepExecution = new StepExecution("step1", jobExecution);
            stepExecution.setWriteCount(8);
            // 리플렉션으로 StepExecution 추가
            addStepExecutionToJobExecution(jobExecution, stepExecution);

            // When
            listener.afterJob(jobExecution);

            // Then
            assertThat(listener.getCurrentExecution().getExecutionStatus()).isEqualTo(SettlementJobStatus.COMPLETED);
            assertThat(listener.getCurrentExecution().getSuccessCount()).isEqualTo(8);

            verify(jobExecutionRepository, times(2)).save(any(SettlementJobExecution.class));
        }

        @Test
        @DisplayName("FAILED 상태로 실행 이력을 업데이트해야 한다")
        void updateExecutionOnFailure() {
            // Given
            when(jobExecutionRepository.findByJobNameAndExecutionDate(any(), any()))
                .thenReturn(Optional.empty());
            when(sellerRepository.countActiveSellers()).thenReturn(10L);
            when(jobExecutionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution exec = invocation.getArgument(0);
                    setId(exec, 1L);
                    return exec;
                });

            listener.beforeJob(jobExecution);

            // Job 실패 상태 설정
            jobExecution.setStatus(BatchStatus.FAILED);
            Throwable exception = new RuntimeException("Database connection failed");
            jobExecution.addFailureException(exception);

            // When
            listener.afterJob(jobExecution);

            // Then
            assertThat(listener.getCurrentExecution().getExecutionStatus()).isEqualTo(SettlementJobStatus.FAILED);
            assertThat(listener.getCurrentExecution().getErrorMessage()).contains("Database connection failed");

            verify(jobExecutionRepository, times(2)).save(any(SettlementJobExecution.class));
        }

        @Test
        @DisplayName("성공률이 정확히 계산되어야 한다")
        void calculateSuccessRate() {
            // Given
            when(jobExecutionRepository.findByJobNameAndExecutionDate(any(), any()))
                .thenReturn(Optional.empty());
            when(sellerRepository.countActiveSellers()).thenReturn(100L);
            when(jobExecutionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution exec = invocation.getArgument(0);
                    setId(exec, 1L);
                    return exec;
                });

            listener.beforeJob(jobExecution);

            jobExecution.setStatus(BatchStatus.COMPLETED);
            StepExecution stepExecution = new StepExecution("step1", jobExecution);
            stepExecution.setWriteCount(95);
            addStepExecutionToJobExecution(jobExecution, stepExecution);

            // When
            listener.afterJob(jobExecution);

            // Then
            // 성공률 = (95 / 100) * 100 = 95%
            assertThat(listener.getCurrentExecution().getSuccessRate()).isEqualTo(95.0);
        }
    }

    @Nested
    @DisplayName("currentExecution이 null인 경우")
    class NullCurrentExecution {

        @Test
        @DisplayName("currentExecution이 null이면 아무것도 수행하지 않아야 한다")
        void doNothingWhenCurrentExecutionIsNull() {
            // Given - beforeJob 호출 없이 직접 afterJob 호출
            jobExecution.setStatus(BatchStatus.COMPLETED);

            // When
            listener.afterJob(jobExecution);

            // Then - 예외 발생 없이 정상 종료
            verify(jobExecutionRepository, never()).save(any(SettlementJobExecution.class));
        }
    }

    @Nested
    @DisplayName("다중 Step 처리")
    class MultipleSteps {

        @Test
        @DisplayName("여러 Step의 통계를 집계해야 한다")
        void aggregateMultipleStepStatistics() {
            // Given
            when(jobExecutionRepository.findByJobNameAndExecutionDate(any(), any()))
                .thenReturn(Optional.empty());
            when(sellerRepository.countActiveSellers()).thenReturn(100L);
            when(jobExecutionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution exec = invocation.getArgument(0);
                    setId(exec, 1L);
                    return exec;
                });

            listener.beforeJob(jobExecution);

            // 여러 Step 실행 시뮬레이션
            StepExecution step1 = new StepExecution("step1", jobExecution);
            step1.setWriteCount(30);
            StepExecution step2 = new StepExecution("step2", jobExecution);
            step2.setWriteCount(40);
            StepExecution step3 = new StepExecution("step3", jobExecution);
            step3.setWriteCount(20);

            // StepExecution을 jobExecution에 추가
            addStepExecutionToJobExecution(jobExecution, step1, step2, step3);

            jobExecution.setStatus(BatchStatus.COMPLETED);

            // When
            listener.afterJob(jobExecution);

            // Then - 집계된 통계 확인 (writeCount 합산: 30 + 40 + 20 = 90)
            assertThat(listener.getCurrentExecution().getSuccessCount()).isEqualTo(90);
        }
    }

    /**
     * JobExecution에 StepExecution을 추가하는 헬퍼 메서드
     */
    private void addStepExecutionToJobExecution(JobExecution jobExecution, StepExecution... stepExecutions) {
        try {
            var field = JobExecution.class.getDeclaredField("stepExecutions");
            field.setAccessible(true);
            java.util.List<StepExecution> stepList = new java.util.ArrayList<>(java.util.Arrays.asList(stepExecutions));
            field.set(jobExecution, stepList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add step executions", e);
        }
    }

    /**
     * 리플렉션으로 ID 설정 (테스트용)
     */
    private void setId(SettlementJobExecution entity, Long id) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }
}

