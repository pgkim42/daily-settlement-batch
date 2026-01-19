package com.company.settlement.batch;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Batch 통합 테스트 공통 설정
 *
 * - @SpringBootTest: 전체 Spring Context 로드
 * - Testcontainers: 테스트용 MySQL 컨테이너 자동 관리
 * - JobLauncherTestUtils: Batch Job 테스트 지원
 *
 * 사용법:
 * <pre>
 * {@code
 * @ExtendWith(SpringExtension.class)
 * class DailySettlementJobIntegrationTest extends AbstractBatchTest {
 *     @Autowired
 *     private JobLauncherTestUtils jobLauncherTestUtils;
 *     ...
 * }
 * }
 * </pre>
 */
@SpringBootTest(classes = com.company.settlement.SettlementApplication.class)
@Testcontainers
public abstract class AbstractBatchTest {

    /**
     * 테스트용 MySQL 컨테이너
     * - 공식 MySQL 이미지 사용 (버전 8)
     * - 테스트 간 격리 보장
     * - 테스트 완료 후 자동 정리
     */
    static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0")
                .asCompatibleSubstituteFor("mysql")
        )
            .withDatabaseName("settlement_test")
            .withUsername("test")
            .withPassword("test");
        MYSQL_CONTAINER.start();
    }

    /**
     * 동적 속성 설정
     * - Testcontainers 컨테이너 정보로 DataSource 설정 동적 구성
     */
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");

        // Batch 설정
        registry.add("spring.batch.job.enabled", () -> "false"); // 테스트 시작 시 Job 자동 실행 방지
        registry.add("spring.batch.initialize-schema", () -> "always");

        // 로깅 레벨 조정 (테스트 시 로그 감소)
        registry.add("logging.level.com.company.settlement", () -> "WARN");
        registry.add("logging.level.org.springframework.batch", () -> "WARN");
        registry.add("logging.level.org.hibernate.SQL", () -> "WARN");
    }
}
