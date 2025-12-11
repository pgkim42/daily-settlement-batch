package com.company.settlement.batch.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch 기본 설정
 *
 * Spring Boot 3.x + Spring Batch 5.x에서는 대부분의 설정이 자동으로 처리됨
 * - JobRepository: DataSource 기반 자동 생성
 * - JobLauncher: 자동 생성
 * - 메타테이블: spring.batch.jdbc.initialize-schema=always 설정으로 자동 생성
 */
@Configuration
public class BatchConfig {

    /**
     * JPA 트랜잭션 매니저
     * Batch Step에서 JPA Entity 처리를 위해 사용
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
