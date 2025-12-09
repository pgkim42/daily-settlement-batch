package com.company.settlement.repository;

import com.company.settlement.domain.entity.SettlementJobExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 정산 잡 실행 이력 Repository
 */
@Repository
public interface SettlementJobExecutionRepository extends JpaRepository<SettlementJobExecution, Long> {

    /**
     * 잡 이름과 실행일로 실행 이력 조회
     * @param jobName 잡 이름
     * @param executionDate 실행일
     * @return 실행 이력
     */
    Optional<SettlementJobExecution> findByJobNameAndExecutionDate(String jobName, LocalDate executionDate);

    /**
     * 잡 이름별 실행 이력 목록 조회
     * @param jobName 잡 이름
     * @param pageable 페이징 정보
     * @return 실행 이력 목록
     */
    Page<SettlementJobExecution> findByJobName(String jobName, Pageable pageable);

    /**
     * 특정 기간의 실행 이력 목록 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 실행 이력 목록
     */
    @Query("SELECT sje FROM SettlementJobExecution sje " +
           "WHERE sje.executionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sje.executionDate DESC, sje.startedAt DESC")
    List<SettlementJobExecution> findByExecutionDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 최근 실행 이력 목록 조회
     * @param limit 조회할 건수
     * @return 실행 이력 목록
     */
    @Query("SELECT sje FROM SettlementJobExecution sje " +
           "ORDER BY sje.executionDate DESC, sje.startedAt DESC " +
           "LIMIT :limit")
    List<SettlementJobExecution> findRecentExecutions(@Param("limit") int limit);

    /**
     * 특정 날짜의 잡 실행 현황 통계
     * @param date 날짜
     * @return 통계 정보 배열 [전체 잡 수, 성공 잡 수, 실패 잡 수, 부분 실패 잡 수]
     */
    @Query("SELECT " +
           "COUNT(*) as totalJobs, " +
           "SUM(CASE WHEN sje.executionStatus = 'COMPLETED' THEN 1 ELSE 0 END) as successJobs, " +
           "SUM(CASE WHEN sje.executionStatus = 'FAILED' THEN 1 ELSE 0 END) as failedJobs, " +
           "SUM(CASE WHEN sje.executionStatus = 'PARTIALLY_FAILED' THEN 1 ELSE 0 END) as partiallyFailedJobs " +
           "FROM SettlementJobExecution sje " +
           "WHERE sje.executionDate = :date")
    Object[] getJobExecutionStatistics(@Param("date") LocalDate date);

    /**
     * 특정 기간의 실행 통계 집계
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 통계 정보 배열 [총 실행 횟수, 평균 성공률, 총 처리 판매자 수, 평균 실행 시간]
     */
    @Query("SELECT " +
           "COUNT(*) as totalExecutions, " +
           "AVG(CAST(sje.successCount AS DOUBLE) / NULLIF(sje.totalSellers, 0) * 100) as avgSuccessRate, " +
           "SUM(sje.totalSellers) as totalSellers, " +
           "AVG(TIMESTAMPDIFF(SECOND, sje.startedAt, sje.completedAt)) as avgExecutionTime " +
           "FROM SettlementJobExecution sje " +
           "WHERE sje.executionDate BETWEEN :startDate AND :endDate " +
           "AND sje.completedAt IS NOT NULL")
    Object[] getExecutionStatistics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 실행 상태별 실행 이력 목록 조회
     * @param executionStatus 실행 상태
     * @param limit 조회할 건수
     * @return 실행 이력 목록
     */
    @Query("SELECT sje FROM SettlementJobExecution sje " +
           "WHERE sje.executionStatus = :executionStatus " +
           "ORDER BY sje.startedAt DESC " +
           "LIMIT :limit")
    List<SettlementJobExecution> findByExecutionStatus(
            @Param("executionStatus") String executionStatus,
            @Param("limit") int limit);

    /**
     * 실행 실패 잡 목록 조회 (에러 메시지 포함)
     * @param fromDate 조회 시작일
     * @return 실행 실패 잡 목록
     */
    @Query("SELECT sje FROM SettlementJobExecution sje " +
           "WHERE sje.executionDate >= :fromDate " +
           "AND sje.executionStatus IN ('FAILED', 'PARTIALLY_FAILED') " +
           "AND sje.errorMessage IS NOT NULL " +
           "ORDER BY sje.executionDate DESC, sje.startedAt DESC")
    List<SettlementJobExecution> findFailedExecutionsWithError(@Param("fromDate") LocalDate fromDate);

    /**
     * 잡 실행 상태별 실행 횟수 조회
     * @param executionStatus 실행 상태
     * @return 실행 횟수
     */
    long countByExecutionStatus(String executionStatus);

    /**
     * 가장 최근 성공한 실행일 조회
     * @param jobName 잡 이름
     * @return 최근 성공일
     */
    @Query("SELECT sje.executionDate FROM SettlementJobExecution sje " +
           "WHERE sje.jobName = :jobName " +
           "AND sje.executionStatus = 'COMPLETED' " +
           "ORDER BY sje.executionDate DESC " +
           "LIMIT 1")
    Optional<LocalDate> findLastSuccessDateByJobName(@Param("jobName") String jobName);
}