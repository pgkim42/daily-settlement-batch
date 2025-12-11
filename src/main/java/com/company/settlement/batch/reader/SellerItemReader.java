package com.company.settlement.batch.reader;

import com.company.settlement.domain.entity.Seller;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 정산 대상 판매자 조회 Reader 설정
 *
 * JpaPagingItemReader를 사용하여 대용량 판매자 데이터를 페이징 처리합니다.
 * - 메모리 효율성: 전체 로드 대신 페이지 단위 조회
 * - 트랜잭션 안정성: 각 페이지가 별도 트랜잭션에서 처리
 */
@Configuration
@Slf4j
public class SellerItemReader {

    private static final int PAGE_SIZE = 100;

    /**
     * 활성 판매자 조회 Reader
     *
     * @StepScope: Job Parameter 바인딩 및 Step별 인스턴스 생성
     *
     * @param entityManagerFactory JPA EntityManagerFactory
     * @param targetDateString 정산 대상 날짜 (Job Parameter)
     * @return JpaPagingItemReader<Seller>
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<Seller> sellerPagingItemReader(
            EntityManagerFactory entityManagerFactory,
            @Value("#{jobParameters['targetDate']}") String targetDateString) {

        log.info("[SellerItemReader] Initializing reader for targetDate: {}", targetDateString);

        return new JpaPagingItemReaderBuilder<Seller>()
            .name("sellerPagingItemReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT s FROM Seller s WHERE s.status = 'ACTIVE' ORDER BY s.id")
            .pageSize(PAGE_SIZE)
            .build();
    }
}
