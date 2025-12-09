# Daily Settlement Batch

Spring Boot 기반의 마켓플레이스 판매자 일일 정산 시스템

## 프로젝트 개요

이 프로젝트는 대규모 마켓플레이스에서 수많은 판매자들의 주문, 결제, 환불 데이터를 처리하여 일일 정산금을 계산하고 지급하는 배치 시스템입니다.

### 주요 기능

- 일일 자동 정산 처리 (매일 새벽 2시 실행)
- 정밀한 금융 계산 (BigDecimal 활용)
- 멱등성 보장 (중복 정산 방지)
- 부분 환불 처리 지원
- 수수료 및 세금 자동 계산
- 정산 결과 조회 및 관리 API

### 기술 스택

- **Backend**: Java 21, Spring Boot 3.5, Spring Batch 5
- **Database**: MySQL 8.0 (Flyway 마이그레이션)
- **Build**: Gradle 8
- **Test**: JUnit 5, Testcontainers, AssertJ
- **Infrastructure**: Docker, Docker Compose

## 🔄 개발 프로세스

이 프로젝트는 정해진 개발 프로세스를 따릅니다. 새로운 기능 개발 시 반드시 아래 단계를 거쳐야 합니다:

1. **PRD 작성** → 2. **PRD 검토 및 보완** → 3. **설계 계획 작성** → 4. **설계 검토 및 보완** → 5. **구현**

자세한 내용은 [WORKFLOW.md](./WORKFLOW.md)를 참고하세요.

### 현재 진행상황
- ✅ PRD 작성 완료
- ✅ 기술 설계 계획 완료
- ⏳ 구현 진행 중

## 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                     전체 구조                           │
│                                                      │
│  주문/결제 → [배치 처리] → 정산금 계산 → 판매자 지급        │
│      ↓              ↓           ↓              ↓      │
│   수집            계산         검증           API      │
└─────────────────────────────────────────────────────┘
```

## 빠른 시작

### 필수 조건

- Java 21+
- Docker & Docker Compose
- MySQL 8.0+ (Docker로 대체 가능)

### 실행 방법

1. **레포지토리 클론**
   ```bash
   git clone https://github.com/[username]/daily-settlement-batch.git
   cd daily-settlement-batch
   ```

2. **Docker로 인프라 실행**
   ```bash
   docker-compose up -d
   ```

3. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   ```

4. **접속 확인**
   - Health Check: http://localhost:8080/actuator/health

## 주요 테이블

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

## 핵심 학습 내용

### 1. 배치 처리
- Spring Batch의 청크(Chunk) 기반 처리
- 대용량 데이터를 안정적으로 처리하는 기법
- 실패 처리 및 재시도 정책

### 2. 금융 시스템 구현
- BigDecimal을 활용한 정밀한 금액 계산
- 멱등성을 위한 데이터베이스 설계
- 트랜잭션 관리 및 데이터 정합성

### 3. 현대적인 개발 방식
- Docker를 이용한 개발 환경 구축
- Flyway를 이용한 데이터베이스 버전 관리
- Testcontainers를 이용한 통합 테스트

## 개발 환경

- **IDE**: IntelliJ IDEA
- **JDK**: Amazon Corretto 21
- **Build Tool**: Gradle 8.x
- **Database**: MySQL 8.0 (Docker)
- **Cache**: Redis (Docker)

## 개발 일지

- [2025-12-08] 프로젝트 기본 구축 완료
  - Spring Boot 프로젝트 설정
  - Docker 개발 환경 구성
  - 데이터베이스 스키마 설계 및 구현