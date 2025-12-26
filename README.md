# Daily Settlement Batch

Spring Boot 기반의 마켓플레이스 판매자 일일 정산 시스템

## 프로젝트 개요

대규모 마켓플레이스에서 수많은 판매자들의 주문, 결제, 환불 데이터를 처리하여 일일 정산금을 계산하고 지급하는 배치 시스템.

### 주요 기능

- 일일 자동 정산 처리 (매일 새벽 2시 실행)
- 정밀한 금융 계산 (BigDecimal 활용)
- 멱등성 보장 (중복 정산 방지)
- 부분 환불 처리 지원
- 수수료 및 세금 자동 계산
- 정산 결과 조회 및 관리 API

### 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.5, Spring Batch 5 |
| Database | MySQL 8.0, Flyway |
| Build | Gradle 8 |
| Test | JUnit 5, AssertJ, ArchUnit |
| Infrastructure | Docker, Docker Compose |

## 빠른 시작

### 필수 조건

- Java 21+
- Docker & Docker Compose

### 실행 방법

```bash
# 1. 레포지토리 클론
git clone https://github.com/pgkim42/daily-settlement-batch.git
cd daily-settlement-batch

# 2. Docker로 MySQL 실행
docker-compose up -d

# 3. 애플리케이션 실행
./gradlew bootRun

# 4. Health Check
curl http://localhost:8080/actuator/health
```

## 프로젝트 구조

```
src/main/java/com/company/settlement/
├── batch/              # Spring Batch (Job, Reader, Processor, Writer)
│   ├── config/         # 배치 설정
│   ├── job/            # Job 정의
│   ├── listener/       # Job/Step Listener
│   ├── processor/      # 정산 계산 로직
│   ├── reader/         # 판매자 조회
│   ├── scheduler/      # 스케줄러 (매일 02:00)
│   ├── service/        # CommissionCalculator
│   └── writer/         # 정산 데이터 저장
├── config/             # JPA 설정
├── controller/         # REST API
├── domain/
│   ├── entity/         # JPA 엔티티 (9개)
│   └── enums/          # Enum (10개)
├── dto/                # Request/Response DTO
├── exception/          # 예외 처리
├── repository/         # Spring Data JPA
└── service/            # 비즈니스 로직
```

## API 엔드포인트

### 판매자용 API

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | /api/settlements | 내 정산 목록 조회 |
| GET | /api/settlements/{id} | 정산 상세 조회 |

### 관리자용 API

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | /api/admin/settlements | 전체 정산 목록 |
| GET | /api/admin/settlements/period | 기간별 조회 |
| GET | /api/admin/settlements/{id} | 정산 상세 |
| GET | /api/admin/settlements/statistics | 통계 |
| POST | /api/admin/settlements/batch/trigger | 배치 수동 실행 |

## 배치 처리

### 처리 흐름

```
┌─────────────────────────────────────────────────────────────┐
│  dailySettlementJob (매일 02:00 KST)                         │
│                                                              │
│  ┌──────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │  Reader  │ -> │  Processor   │ -> │    Writer    │       │
│  │ 판매자   │    │ 정산금 계산   │    │ 정산 데이터  │       │
│  │ 목록조회 │    │              │    │    저장     │       │
│  └──────────┘    └──────────────┘    └──────────────┘       │
│                                                              │
│  Chunk Size: 100 | Skip Limit: 100                          │
└─────────────────────────────────────────────────────────────┘
```

### 정산 계산 공식

```
순매출액 = 총매출 - 환불액
수수료 = 순매출액 × 수수료율
부가세 = 수수료 × 10%
정산액 = 순매출액 - 수수료 - 부가세 + 조정액
```

### 멱등성 보장

1. SettlementProcessor에서 중복 정산 체크
2. DB 유니크 제약조건 (seller_id + cycle_type + period_start + period_end)
3. Spring Batch Job Instance (targetDate 기반)

## 데이터베이스 스키마

| 테이블 | 설명 |
|--------|------|
| sellers | 판매자 정보 |
| orders | 주문 정보 |
| order_items | 주문 상품 정보 |
| payments | 결제 정보 |
| refunds | 환불 정보 |
| settlements | 정산 정보 |
| settlement_items | 정산 항목 상세 |
| settlement_job_executions | 배치 실행 이력 |

## 테스트

```bash
./gradlew test
```

### 테스트 현황

- 단위 테스트 33개+
- CommissionCalculator: 수수료/부가세 계산 검증 (17개)
- Controller: MockMvc 기반 API 테스트 (13개)
- Repository: 쿼리 메서드 검증

## 개발 환경

| 항목 | 버전 |
|------|------|
| IDE | IntelliJ IDEA |
| JDK | Amazon Corretto 21 |
| Build | Gradle 8.x |
| Database | MySQL 8.0 (Docker) |

## 개발 일지

- 2025-12-08: 프로젝트 초기 설정 및 DB 스키마 구현
- 2025-12-09: 도메인 계층 구현 (9개 엔티티, 8개 Repository)
- 2025-12-11: Spring Batch Job 구현 (Reader/Processor/Writer)
- 2025-12-12: API 및 스케줄러 구현, 코드 리뷰 기반 리팩터링

## 문서

- [PRD (제품 요구사항)](./docs/PRD_제품요구사항문서.md)
- [도메인 정의서](./docs/도메인%20정의서.md)
- [ERD 및 테이블 설계](./docs/ERD%20및%20테이블%20설계.md)
- [개발 워크플로우](./WORKFLOW.md)
