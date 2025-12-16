package com.company.settlement.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.junit.jupiter.api.BeforeEach;

/**
 * Batch Job 통합 테스트 추상 클래스
 * 
 * Testcontainers MySQL 기반 테스트 환경 제공
 * Spring Batch Test 유틸리티 제공
 */
@SpringBootTest
@SpringBatchTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractBatchTest {

    static final MySQLContainer<?> mysql;

    static {
        mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("settlement_test")
                .withUsername("test")
                .withPassword("test")
                .withUrlParam("useSSL", "false")
                .withUrlParam("allowPublicKeyRetrieval", "true")
                .withUrlParam("serverTimezone", "Asia/Seoul")
                .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--skip-ssl"
                );
        mysql.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // DDL 설정 (테스트용 스키마 생성)
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");  // Flyway 비활성화
        
        // Batch 설정
        registry.add("spring.batch.job.enabled", () -> "false");  // 자동 실행 방지
        registry.add("spring.batch.jdbc.initialize-schema", () -> "always");  // Batch 테이블 생성
        registry.add("settlement.scheduler.enabled", () -> "false");  // 스케줄러 비활성화
    }

    @Autowired
    protected JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    protected JobRepositoryTestUtils jobRepositoryTestUtils;

    /**
     * 각 테스트 전에 Job 실행 이력 초기화
     */
    @BeforeEach
    void cleanUpJobExecutions() {
        if (jobRepositoryTestUtils != null) {
            jobRepositoryTestUtils.removeJobExecutions();
        }
    }

    /**
     * 테스트할 Job을 설정
     * 
     * @param job 테스트할 Job
     */
    protected void setJob(Job job) {
        jobLauncherTestUtils.setJob(job);
    }
}
