---
title: 마켓플레이스 정산 시스템 - README
created: 2025-12-01
updated: 2025-12-01
tags: [project, settlement, readme, portfolio]
category: Projects
status: draft
---

# Marketplace Settlement System

마켓플레이스 판매자 정산 시스템

> 주문/결제/환불 데이터를 기반으로 판매자별 정산 금액을 산출하는 배치 기반 정산 시스템

---

## 1. 프로젝트 소개

### 배경

마켓플레이스에서 발생한 거래에 대해 판매자에게 정확한 금액을 지급하는 것은 플랫폼의 핵심 기능이다. 이 프로젝트는 **"어제 판매자 A가 얼마를 벌었고, 우리는 얼마를 지급해야 하는가?"** 라는 질문에 답하기 위한 정산 시스템을 구현한다.

### 목표

- 결제/환불 데이터를 기반으로 판매자별 정산 금액 산출
- 배치 기반 일별/주별 정산 처리
- 재실행 시에도 중복 정산이 발생하지 않는 멱등한 시스템
- 정산 내역의 추적 가능성(Traceability) 확보

### 프로젝트 특징

이 프로젝트는 다음에 초점을 맞추었다:

```
"실무적으로 완벽한 정답"보다
"1년 차 개발자 포트폴리오에서 설계 역량을 가장 잘 드러낼 수 있는 선택"
```

- **설계 의사결정 문서화**: 왜 이 기술을 선택했는지, 대안은 무엇이었는지
- **도메인 모델링**: DDD 관점의 Bounded Context와 Aggregate 설계
- **테스트 가능한 구조**: 명확한 입력/출력으로 검증 가능한 비즈니스 로직

---

## 2. 주요 기능

### 핵심 기능

| 기능 | 설명 |
|------|------|
| 일별 정산 배치 | 매일 02:00에 전일 결제 건에 대해 판매자별 정산 실행 |
| 환불 반영 | 환불 발생 시점 기준으로 해당 정산에 차감 항목 생성 |
| 수수료 계산 | 판매자별 수수료율 적용, 부가세 자동 계산 |
| 수동 조정 | 프로모션 보전, 수수료 조정 등 수동 항목 추가 |
| 재정산 | 오류 발생 시 기존 정산 취소 후 재계산 |

### 정산 금액 계산

```
payout_amount = gross_sales_amount
                - refund_amount
                - commission_amount
                - tax_amount
                + adjustment_amount
```

### 상태 흐름

```
Settlement:
  PENDING → CONFIRMED → PAID
      ↓         ↓
   CANCELED  CANCELED (재정산)
```

---

## 3. 기술 스택

### Backend

| 기술              | 버전    | 용도        |
| --------------- | ----- | --------- |
| Java            | 17+   | 언어        |
| Spring Boot     | 3.5.x | 프레임워크     |
| Spring Batch    | 5.x   | 배치 처리     |
| Spring Data JPA | 3.x   | ORM       |
| QueryDSL        | 5.x   | 타입 세이프 쿼리 |

### Database

| 기술 | 버전 | 용도 |
|------|------|------|
| MySQL | 8.0+ | 메인 DB |
| H2 | 2.x | 테스트 DB |

### Testing

| 기술 | 용도 |
|------|------|
| JUnit 5 | 단위/통합 테스트 |
| AssertJ | Assertion |
| Testcontainers | DB 통합 테스트 |

### Build & Tools

| 기술 | 용도 |
|------|------|
| Gradle | 빌드 도구 |
| Docker Compose | 로컬 개발 환경 |

---

## 4. 아키텍처

### 시스템 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                        Settlement System                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │   Scheduler  │───▶│  Batch Job   │───▶│  Settlement  │       │
│  │  (Cron)      │    │ (Spring Batch)│    │   Service    │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│                             │                    │               │
│                             ▼                    ▼               │
│                      ┌──────────────┐    ┌──────────────┐       │
│                      │  Calculator  │    │  Repository  │       │
│                      │  (Domain)    │    │  (JPA)       │       │
│                      └──────────────┘    └──────────────┘       │
│                                                  │               │
└──────────────────────────────────────────────────│───────────────┘
                                                   │
                                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                           MySQL                                  │
│  ┌─────────┐ ┌─────────┐ ┌────────────┐ ┌──────────────────┐   │
│  │ order   │ │ payment │ │ settlement │ │ settlement_item  │   │
│  └─────────┘ └─────────┘ └────────────┘ └──────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 레이어 구조

```
src/main/java/com/example/settlement/
├── batch/                    # Spring Batch Job/Step 정의
│   ├── job/
│   │   └── DailySettlementJobConfig.java
│   ├── reader/
│   ├── processor/
│   └── writer/
├── domain/                   # 도메인 모델
│   ├── settlement/
│   │   ├── Settlement.java
│   │   ├── SettlementItem.java
│   │   └── SettlementCalculator.java
│   └── order/
├── application/              # 애플리케이션 서비스
│   └── SettlementService.java
├── infrastructure/           # 인프라 (Repository 구현체)
│   └── repository/
└── interfaces/               # API Controller
    └── admin/
```

### Bounded Context

```
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│   Upstream (Order Context)      │     │   Settlement Context            │
│                                 │     │                                 │
│  - Order                        │     │  - Settlement                   │
│  - OrderItem                    │────▶│  - SettlementItem               │
│  - Payment                      │     │  - SettlementJobExecution       │
│  - Refund                       │     │                                 │
└─────────────────────────────────┘     └─────────────────────────────────┘

관계: Conformist (Settlement가 Upstream 모델을 따름)
통신: 배치 시점 DB 조회 (동기)
```

---

## 5. 설계 의사결정

### 5.1 정합성 (Consistency)

#### BigDecimal 사용

```java
// Bad: 부동소수점 오차 발생 가능
double commission = 10000 * 0.1;  // 999.9999999...?

// Good: 정확한 금액 계산
BigDecimal commission = new BigDecimal("10000")
    .multiply(new BigDecimal("0.1"));  // 정확히 1000
```

**선택 이유**: 금액 계산에서 1원의 오차도 허용되지 않음. 부동소수점 연산의 정밀도 문제를 원천 차단.

#### CHECK 제약조건

```sql
CONSTRAINT chk_settlement_amounts CHECK (
    gross_sales_amount >= 0 AND
    refund_amount >= 0 AND
    commission_amount >= 0
)
```

**선택 이유**: 애플리케이션 버그로 잘못된 데이터가 들어가는 것을 DB 레벨에서 방지.

### 5.2 멱등성 (Idempotency)

#### Partial Unique Index

```sql
-- 동일 (seller, cycle, period)에 활성 상태 정산은 1건만 허용
CREATE UNIQUE INDEX uk_settlement_active
ON settlement (seller_id, cycle_type, period_start, period_end, status_active_flag);
```

**동작 방식**:
- `status_active_flag`: PENDING/CONFIRMED/PAID면 1, CANCELED면 NULL
- NULL은 유니크 제약에서 제외됨
- 재정산 시 기존 건 CANCELED 처리 후 새로 INSERT 가능

**선택 이유**: 물리 삭제 없이 이력을 보존하면서 중복 정산 방지.

#### SettlementItem 중복 방지

```sql
UNIQUE KEY uk_settlement_item_source (settlement_id, source_type, source_id)
```

**선택 이유**: 같은 OrderItem이 한 정산에 두 번 들어가는 것을 DB 레벨에서 방지.

### 5.3 재실행 가능성 (Restartability)

#### Spring Batch Chunk 처리

```java
@Bean
public Step settlementStep() {
    return stepBuilderFactory.get("settlementStep")
        .<Seller, Settlement>chunk(100)  // 100건씩 처리
        .reader(sellerReader())
        .processor(settlementProcessor())
        .writer(settlementWriter())
        .faultTolerant()
        .skipLimit(10)  // 최대 10건 스킵 허용
        .skip(SettlementException.class)
        .build();
}
```

**선택 이유**:
- Chunk 단위 트랜잭션으로 부분 실패 시 영향 최소화
- Skip 정책으로 일부 오류가 전체 배치를 막지 않음
- JobExecution 기록으로 실패 지점부터 재시작 가능

### 5.4 트레이드오프: 왜 Kafka를 안 썼는가?

| 선택지 | 장점 | 단점 |
|--------|------|------|
| **Kafka 이벤트 기반** | 실시간 처리, 느슨한 결합 | 복잡도 증가, 이벤트 순서 보장 어려움 |
| **배치 DB 조회** | 단순함, 디버깅 용이, 시점 정합성 | 실시간 아님, DB 부하 |

**최종 선택**: 배치 시점 DB 조회

**이유**:
1. **정산의 특성**: 정산은 "일정 기간이 마감된 후" 처리하는 것이 자연스러움
2. **시점 정합성**: 배치 시작 시점의 스냅샷으로 계산하면 "정산 중 새 주문 발생" 문제 회피
3. **디버깅 용이**: 문제 발생 시 동일 조건으로 쿼리 재실행하여 원인 파악 가능
4. **MVP 적합성**: 1년차 포트폴리오에서 Kafka 도입은 과한 복잡도

```
"이벤트 기반 아키텍처가 더 있어 보이지만,
배치 정산에서는 시점 기준 DB 조회가 더 명확하다."
```

### 5.5 트레이드오프: 왜 이벤트 소싱을 안 썼는가?

| 선택지 | 장점 | 단점 |
|--------|------|------|
| **이벤트 소싱** | 완벽한 이력 추적, 시점 복원 가능 | 높은 복잡도, 러닝커브 |
| **스냅샷 기반** | 단순함, 익숙함, 즉시 조회 가능 | 과거 상태 복원 제한적 |

**최종 선택**: 스냅샷 기반

**이유**:
1. 정산 결과는 "확정 후 변경 불가"가 원칙 → 복원할 필요 적음
2. SettlementItem에 source_id로 원천 추적 가능 → 충분한 추적성
3. 이벤트 소싱 도입 시 CQRS 필요 → MVP에 과한 복잡도

---

## 6. 실행 방법

### 6.1 사전 요구사항

- Java 17+
- Docker & Docker Compose
- Gradle 8.x

### 6.2 로컬 환경 구성

#### MySQL 실행 (Docker Compose)

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: settlement-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: settlement
      MYSQL_USER: settlement
      MYSQL_PASSWORD: settlement
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init:/docker-entrypoint-initdb.d

volumes:
  mysql-data:
```

```bash
# MySQL 컨테이너 실행
docker-compose up -d

# 상태 확인
docker-compose ps
```

### 6.3 스키마 생성

```bash
# Flyway 마이그레이션 (애플리케이션 실행 시 자동)
./gradlew bootRun

# 또는 수동 실행
./gradlew flywayMigrate
```

### 6.4 더미 데이터 생성

#### data.sql 사용

```sql
-- src/main/resources/data.sql

-- Seller 데이터
INSERT INTO seller (id, seller_code, seller_name, commission_rate, status, settlement_cycle)
VALUES
  (1, 'S001', '판매자A', 0.10, 'ACTIVE', 'DAILY'),
  (2, 'S002', '판매자B', 0.08, 'ACTIVE', 'DAILY'),
  (3, 'S003', '판매자C', 0.10, 'ACTIVE', 'DAILY');

-- Order & Payment 데이터 (정산 대상일: 2025-01-15)
INSERT INTO `order` (id, order_number, seller_id, status, total_amount, ordered_at)
VALUES
  (1, 'ORD-001', 1, 'PAID', 50000, '2025-01-15 10:00:00'),
  (2, 'ORD-002', 1, 'PAID', 30000, '2025-01-15 14:30:00'),
  (3, 'ORD-003', 2, 'PAID', 100000, '2025-01-15 09:00:00');

INSERT INTO order_item (id, order_id, product_name, quantity, unit_price, total_price, status)
VALUES
  (1, 1, '상품A', 2, 25000, 50000, 'ACTIVE'),
  (2, 2, '상품B', 1, 30000, 30000, 'ACTIVE'),
  (3, 3, '상품C', 2, 50000, 100000, 'ACTIVE');

INSERT INTO payment (id, payment_key, order_id, amount, status, paid_at)
VALUES
  (1, 'PAY-001', 1, 50000, 'CONFIRMED', '2025-01-15 10:00:00'),
  (2, 'PAY-002', 2, 30000, 'CONFIRMED', '2025-01-15 14:30:00'),
  (3, 'PAY-003', 3, 100000, 'CONFIRMED', '2025-01-15 09:00:00');
```

#### 애플리케이션 설정

```yaml
# application.yml
spring:
  sql:
    init:
      mode: always  # 개발 환경에서만
      data-locations: classpath:data.sql
```

#### 프로그래밍 방식 (DataLoader)

```java
@Component
@Profile("local")
@RequiredArgsConstructor
public class TestDataLoader implements CommandLineRunner {

    private final SellerRepository sellerRepository;
    private final OrderRepository orderRepository;
    // ...

    @Override
    @Transactional
    public void run(String... args) {
        if (sellerRepository.count() > 0) {
            return;  // 이미 데이터 있으면 스킵
        }

        // Seller 생성
        Seller sellerA = createSeller("S001", "판매자A", 0.10);
        Seller sellerB = createSeller("S002", "판매자B", 0.08);

        // Order 생성
        LocalDate targetDate = LocalDate.of(2025, 1, 15);
        createPaidOrder(sellerA, 50000, targetDate.atTime(10, 0));
        createPaidOrder(sellerA, 30000, targetDate.atTime(14, 30));
        createPaidOrder(sellerB, 100000, targetDate.atTime(9, 0));

        log.info("테스트 데이터 생성 완료");
    }
}
```

### 6.5 Spring Batch Job 실행

#### 스케줄 자동 실행

```yaml
# application.yml
spring:
  batch:
    job:
      enabled: false  # 자동 실행 비활성화 (스케줄러가 제어)

settlement:
  batch:
    cron: "0 0 2 * * *"  # 매일 02:00
```

#### 수동 실행 (CLI)

```bash
# 특정 날짜 정산 실행
./gradlew bootRun --args='--spring.batch.job.names=dailySettlementJob --targetDate=2025-01-15'

# 또는 JAR 실행
java -jar build/libs/settlement-0.0.1.jar \
  --spring.batch.job.names=dailySettlementJob \
  --targetDate=2025-01-15 \
  --cycleType=DAILY
```

#### Job Parameter 설명

| Parameter | 필수 | 기본값 | 설명 |
|-----------|------|--------|------|
| targetDate | O | - | 정산 대상 일자 (yyyy-MM-dd) |
| cycleType | X | DAILY | 정산 주기 (DAILY/WEEKLY) |
| sellerId | X | null | 특정 판매자만 정산 (재정산 시) |

#### REST API 실행 (Admin)

```bash
# 정산 배치 트리거
curl -X POST http://localhost:8080/api/admin/settlements/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "targetDate": "2025-01-15",
    "cycleType": "DAILY"
  }'

# 실행 결과 조회
curl http://localhost:8080/api/admin/settlements/jobs/{jobExecutionId}
```

### 6.6 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 통합 테스트만 (Testcontainers 사용)
./gradlew integrationTest

# 특정 테스트 클래스
./gradlew test --tests "SettlementCalculatorTest"

# 테스트 리포트 확인
open build/reports/tests/test/index.html
```

---

## 7. 향후 개선 아이디어

### 7.1 이벤트 기반 정산 (v2)

현재 배치 기반에서 이벤트 기반으로 전환하여 준실시간 정산 지원.

```
[현재 - v1]
주문 발생 → (대기) → 익일 02:00 배치 → 정산 생성

[향후 - v2]
주문 발생 → OrderPaidEvent 발행 → 정산 대상 큐에 적재 → 정산 Worker 처리
```

**필요 기술**: Kafka, Redis(정산 대기열), Debezium(CDC)

**트레이드오프**:
- 장점: 준실시간 정산, 배치 부하 분산
- 단점: 이벤트 순서 보장, At-least-once 처리, 복잡도 증가

### 7.2 정산 결과 대시보드/어드민 UI

```
┌─────────────────────────────────────────────────────────────┐
│  정산 관리 대시보드                                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [오늘의 정산 현황]                                          │
│  ┌─────────────┬─────────────┬─────────────┐              │
│  │ 총 정산액    │ 총 환불액    │ 총 수수료    │              │
│  │ 12,500,000  │ 1,200,000   │ 1,130,000   │              │
│  └─────────────┴─────────────┴─────────────┘              │
│                                                             │
│  [판매자별 정산 목록]                                        │
│  ┌─────┬────────┬─────────┬────────┬────────┐            │
│  │ ID  │ 판매자  │ 총매출   │ 지급액  │ 상태    │            │
│  ├─────┼────────┼─────────┼────────┼────────┤            │
│  │ 101 │ 판매자A │ 500,000 │ 445,000│CONFIRMED│            │
│  │ 102 │ 판매자B │ 300,000 │ 276,000│PENDING  │            │
│  └─────┴────────┴─────────┴────────┴────────┘            │
│                                                             │
│  [액션]                                                     │
│  [재정산] [일괄 확정] [엑셀 다운로드]                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**필요 기술**: React/Next.js, Recharts(차트), TanStack Table

### 7.3 알림/비정상 패턴 감지

#### 알림 시나리오

| 시나리오 | 조건 | 알림 방식 |
|----------|------|-----------|
| 배치 실패 | JobExecution.status = FAILED | Slack + Email |
| 음수 정산 | payout_amount < 0 | Slack |
| 이상 환불률 | 환불률 > 30% | Slack |
| 대량 정산 지연 | 처리 시간 > 1시간 | PagerDuty |

#### 구현 아이디어

```java
@Component
public class SettlementAlertHandler {

    @EventListener
    public void onNegativeSettlement(NegativeSettlementEvent event) {
        slackNotifier.send(
            "#settlement-alert",
            String.format("음수 정산 발생: Seller=%s, Amount=%s",
                event.getSellerId(), event.getPayoutAmount())
        );
    }

    @EventListener
    public void onHighRefundRate(HighRefundRateEvent event) {
        if (event.getRefundRate().compareTo(new BigDecimal("0.3")) > 0) {
            slackNotifier.send(
                "#settlement-alert",
                String.format("높은 환불률 감지: Seller=%s, Rate=%.1f%%",
                    event.getSellerId(), event.getRefundRate().multiply(100))
            );
        }
    }
}
```

### 7.4 정산 리포트 자동화

```java
@Scheduled(cron = "0 0 9 * * MON")  // 매주 월요일 09:00
public void generateWeeklyReport() {
    WeeklySettlementReport report = reportGenerator.generate(
        LocalDate.now().minusWeeks(1),
        LocalDate.now().minusDays(1)
    );

    // PDF 생성
    byte[] pdf = pdfGenerator.generate(report);

    // 이메일 발송
    emailSender.send(
        "settlement-team@example.com",
        "주간 정산 리포트",
        pdf
    );
}
```

### 7.5 성능 개선

| 개선 항목 | 현재 | 목표 | 방법 |
|-----------|------|------|------|
| 배치 처리량 | 100건/초 | 1,000건/초 | 파티셔닝, 병렬 Step |
| 정산 조회 | 500ms | 50ms | Redis 캐싱, 요약 테이블 |
| DB 부하 | 높음 | 중간 | Read Replica 분리 |

---

## 8. 설계 문서 목록

| 문서 | 설명 |
|------|------|
| [[도메인 정의서]] | Bounded Context, Ubiquitous Language, Aggregate 설계 |
| [[정산 규칙 및 정책]] | 정산 기준, 환불 반영, 금액 계산 공식 |
| [[유스케이스 명세서]] | UC-S1~S5 상세 시나리오 |
| [[ERD 및 테이블 설계]] | 8개 테이블 DDL, 인덱스, 제약조건 |
| [[테스트 시나리오 및 예제 데이터]] | 8개 테스트 케이스, JUnit 코드 예시 |

---

## 9. 참고 자료

### 공식 문서

- [Spring Batch Reference](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [JPA Specification](https://jakarta.ee/specifications/persistence/)

### 도서

- "도메인 주도 설계" - Eric Evans
- "가상 면접 사례로 배우는 대규모 시스템 설계 기초" - Alex Xu

### 블로그/아티클

- [우아한형제들 정산 시스템](https://techblog.woowahan.com/)
- [토스 페이먼츠 정산](https://toss.tech/)

---

## License

This project is for portfolio purposes.

---

## 관련 문서

- [[사이드 프로젝트 공통 가이드]]
