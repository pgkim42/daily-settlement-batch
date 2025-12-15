package com.company.settlement.repository;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Repository 통합 테스트 추상 클래스
 * 
 * Testcontainers MySQL 기반 테스트 환경 제공
 * 
 * Singleton Container 패턴 적용:
 * - 모든 테스트 클래스가 단일 MySQL 컨테이너를 공유
 * - JVM 종료 시 Ryuk이 자동으로 컨테이너 정리
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractRepositoryTest {

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
    }
}
