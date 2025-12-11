# TODO List

## 프로젝트 현황

**프로젝트**: 마켓플레이스 판매자 일일 정산 시스템
**마지막 업데이트**: 2025-12-12
**현재 단계**: 스케줄러, API, 테스트 데이터 구현 완료

## 완료된 작업 ✅

### Phase 1: 기반 구축
- [x] PRD(제품 요구사항 문서) 작성 (2025-12-08)
  - 위치: `docs/PRD_제품요구사양문서.md`
- [x] 기술 설계 PLAN 작성 (2025-12-08)
  - 위치: Claude 대화 기록
- [x] 도메인 Entity 클래스 구현 (2025-12-09)
  - 패키지: `src/main/java/com/company/settlement/domain/entity/`
  - 구현된 Entity: Seller, Order, OrderItem, Payment, Refund, Settlement, SettlementItem, SettlementJobExecution
- [x] Repository 인터페이스 구현 (2025-12-09)
  - 패키지: `src/main/java/com/company/settlement/repository/`
  - 복잡한 JPQL 쿼리 구현
  - 멱등성 보장을 위한 쿼리 추가
  - BigDecimal 기반 금액 집계 쿼리

### Phase 2: 비즈니스 로직 구현
- [x] Spring Batch Job 구현 (2025-12-11)
  - 패키지: `src/main/java/com/company/settlement/batch/`
  - **BatchConfig**: 트랜잭션 매니저 설정
  - **DailySettlementJobConfig**: Job/Step 정의 (chunk=100)
  - **SellerItemReader**: @StepScope 기반 활성 판매자 조회
  - **SettlementProcessor**: 정산 계산 핵심 로직
    - 멱등성 체크 (동일 기간 중복 정산 방지)
    - 수수료/부가세/정산액 계산 (BigDecimal, HALF_UP)
  - **SettlementWriter**: Settlement + SettlementItem 배치 저장
  - **JobExecutionListener**: 실행 이력 관리
  - **SettlementItemSkipListener**: Skip 처리 로깅
  - **예외 클래스**: SettlementAlreadyExistsException, SettlementProcessingException
  - **DTO**: SettlementContext (Processor → Writer 전달용)
- [x] OrderRepository Fetch Join 쿼리 추가 (2025-12-11)
  - N+1 문제 해결을 위한 `findSettlementTargetOrdersWithItems()` 메서드

### Phase 3: API 및 스케줄링
- [x] SettlementScheduler 구현 (2025-12-12)
  - `src/main/java/com/company/settlement/batch/scheduler/SettlementScheduler.java`
  - @Scheduled를 이용한 매일 02:00 KST 실행
  - @ConditionalOnProperty로 활성화 제어
  - `settlement.scheduler.enabled` 설정 추가
- [x] 정산 결과 조회 API 구현 (2025-12-12)
  - **SettlementController**: 판매자용 정산 조회 API
  - **AdminSettlementController**: 관리자용 정산/통계/배치 트리거 API
  - **Service 계층**: 인터페이스와 구현체 분리 (3쌍)
  - **DTO**: record 패턴 활용 (Request 1개, Response 7개)
- [x] 예외 처리 및 전역 핸들러 구현 (2025-12-12)
  - SettlementNotFoundException, SettlementAccessDeniedException
  - BatchAlreadyRunningException, GlobalExceptionHandler
  - RFC 7807 ProblemDetail 응답 형식
- [x] SettlementRepository 확장 (2025-12-12)
  - Fetch Join 쿼리 6개 추가 (N+1 문제 해결)
  - 통계 집계 쿼리 추가
- [x] 테스트 데이터 생성 (2025-12-12)
  - `V2__Insert_test_data.sql` Flyway 마이그레이션
  - 판매자 3명, 주문 18건, 환불 4건

## 다음 할 일 📋

### Phase 4: 통합 테스트 작성 (우선순위 높음)
- [ ] **Repository 테스트**
  - `src/test/java/com/company/settlement/repository/`
  - Testcontainers 사용
  - 복잡한 JPQL 쿼리 테스트

- [ ] **Service 테스트**
  - 정산 계산 로직 테스트
  - 경계 조건 테스트 (0원, 최대값 등)

- [ ] **Batch Job 테스트**
  - JobLauncherTestUtils 사용
  - 통합 시나리오 테스트

### Phase 5: 모니터링 (우선순위 낮음)

- [ ] **Actuator 설정**
  - Health check
  - Job 실행 상태 모니터링

- [ ] **로그 설정**
  - Batch 실행 로그
  - 에러 발생 시 상세 로그

## 기술적인 주의사항 ⚠️

### 금액 계산
- **절대 double 사용 금지**: 항상 BigDecimal 사용
- **RoundingMode**: 반드시 HALF_UP 사용 (은행 기준)
- **단위 테스트**: 모든 금액 계산 로직은 100% 커버리지 목표

### 멱등성 보장
- **복합 유니크 제약조건**: (seller_id, cycle_type, period_start, period_end, status)
- **배치 실행 전 중복 확인**
- **@Version 낙관적 잠금 고려**

### 대용량 처리
- **Chunk 사이즈**: 100으로 시작하고 튜닝
- **JPA 배치 설정**: `spring.jpa.properties.hibernate.jdbc.batch_size=100`
- **메모리 관리**: Cursor 기반 Reader 사용

### 트랜잭션 경계
- **Chunk 단위 트랜잭션**: Spring Batch 기본 설정 활용
- **독립적 트랜잭션 필요 시**: @Transactional(propagation = Propagation.REQUIRES_NEW)

## 다음 세션 시작 시 확인할 것 🔍

1. MySQL Docker 컨테이너 실행 상태 확인
   ```bash
   docker-compose ps
   ```

2. 마지막 커밋 상태 확인
   ```bash
   git log --oneline -5
   ```

3. 다음 작업 시작점
   - 통합 테스트 코드 작성
   - `src/test/java/com/company/settlement/` 패키지

4. 배치 Job 실행 테스트
   ```bash
   ./gradlew bootRun --args='--spring.batch.job.name=dailySettlementJob targetDate=2024-01-15'
   ```

5. API 테스트 (서버 실행 후)
   ```bash
   # 판매자 정산 목록 조회
   curl -H "X-Seller-Id: 1" http://localhost:8080/api/settlements

   # 관리자 통계 조회
   curl "http://localhost:8080/api/admin/settlements/statistics?start=2024-01-01&end=2024-01-31"
   ```

## 참고 자료 📚

- Spring Batch 공식 문서: https://docs.spring.io/spring-batch/docs/current/reference/html/
- WORKFLOW.md: 개발 프로세스 상세
- docs/ 폴더의 설계 문서들

## 메모 📝

- SettlementJobExecution 테이블을 활용한 배치 실행 이력 관리
- 실패 시 재실행 가능한 구조 설계
- 테스트 시나리오: `docs/테스트 시나리오 및 예제 데이터.md` 참고