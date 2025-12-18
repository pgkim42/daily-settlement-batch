package com.company.settlement;

import com.company.settlement.repository.AbstractRepositoryTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전 계층 통합 테스트를 위한 추상 베이스 클래스
 * 
 * AbstractRepositoryTest를 상속받아 Testcontainers MySQL 환경을 공유합니다.
 * @SpringBootTest를 통해 전체 애플리케이션 컨텍스트를 로드합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest extends AbstractRepositoryTest {
}
