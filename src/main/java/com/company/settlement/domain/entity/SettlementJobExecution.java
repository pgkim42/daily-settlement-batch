package com.company.settlement.domain.entity;

import com.company.settlement.domain.enums.SettlementJobStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정산 잡 실행 이력 Entity
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "settlement_job_executions",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_job_execution",
                            columnNames = {"job_name", "execution_date"})
       },
       indexes = {
           @Index(name = "ix_job_executions_date", columnList = "execution_date"),
           @Index(name = "ix_job_executions_status", columnList = "execution_status")
       })
@Comment("정산 배치 잡 실행 이력")
public class SettlementJobExecution extends BaseEntity {

    @Column(name = "job_name", nullable = false, length = 100)
    @Comment("잡 이름 (ex: dailySettlementJob)")
    private String jobName;

    @Column(name = "execution_date", nullable = false)
    @Comment("실행 대상일")
    private LocalDate executionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false)
    @Comment("실행 상태")
    private SettlementJobStatus executionStatus;

    @Column(name = "total_sellers", nullable = false)
    @Comment("전체 판매자 수")
    private Integer totalSellers;

    @Column(name = "success_count", nullable = false)
    @Comment("성공한 판매자 수")
    private Integer successCount;

    @Column(name = "failure_count", nullable = false)
    @Comment("실패한 판매자 수")
    private Integer failureCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @Comment("전체 에러 메시지")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    @Comment("시작 시간")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    @Comment("완료 시간")
    private LocalDateTime completedAt;

    @Builder
    public SettlementJobExecution(String jobName, LocalDate executionDate,
                                  SettlementJobStatus executionStatus, Integer totalSellers) {
        this.jobName = jobName;
        this.executionDate = executionDate;
        this.executionStatus = executionStatus != null ? executionStatus : SettlementJobStatus.STARTED;
        this.totalSellers = totalSellers != null ? totalSellers : 0;
        this.successCount = 0;
        this.failureCount = 0;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 잡 시작
     */
    public void start(Integer totalSellers) {
        this.executionStatus = SettlementJobStatus.STARTED;
        this.totalSellers = totalSellers;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 잡 완료
     */
    public void complete(Integer successCount, Integer failureCount) {
        this.executionStatus = failureCount > 0
            ? SettlementJobStatus.PARTIALLY_FAILED
            : SettlementJobStatus.COMPLETED;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 잡 실패
     */
    public void fail(String errorMessage) {
        this.executionStatus = SettlementJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 성공 카운트 증가
     */
    public void incrementSuccessCount() {
        this.successCount++;
    }

    /**
     * 실패 카운트 증가 및 에러 메시지 추가
     */
    public void incrementFailureCount(String errorMessage) {
        this.failureCount++;
        if (this.errorMessage == null) {
            this.errorMessage = errorMessage;
        } else {
            this.errorMessage += "\n" + errorMessage;
        }
    }

    /**
     * 실행 중인지 확인
     */
    public boolean isRunning() {
        return this.executionStatus == SettlementJobStatus.STARTED;
    }

    /**
     * 완료된지 확인
     */
    public boolean isCompleted() {
        return this.executionStatus == SettlementJobStatus.COMPLETED
            || this.executionStatus == SettlementJobStatus.PARTIALLY_FAILED
            || this.executionStatus == SettlementJobStatus.FAILED;
    }

    /**
     * 성공률 계산
     */
    public double getSuccessRate() {
        if (totalSellers == 0) return 0.0;
        return (double) successCount / totalSellers * 100;
    }

    /**
     * 실행 시간 계산 (초)
     */
    public long getExecutionTimeInSeconds() {
        if (completedAt == null) return 0;
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }
}