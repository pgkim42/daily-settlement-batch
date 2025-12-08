---
title: 마켓플레이스 정산 시스템 - ERD 및 테이블 설계
created: 2025-12-01
updated: 2025-12-01
tags: [project, settlement, erd, database, schema]
category: Projects
status: draft
---

# ERD 및 테이블 설계

## 개요

본 문서는 판매자 정산 시스템의 데이터베이스 스키마 설계를 설명한다. 코드 리뷰 전 팀 내 공유를 목적으로 작성되었으며, 각 테이블의 역할과 관계, 그리고 정산 도메인 특성에 맞춘 설계 의도를 담고 있다.

### 설계 원칙

| 원칙 | 적용 |
|------|------|
| 정합성 | 금액 계산의 일관성 보장 (BigDecimal, CHECK 제약) |
| 추적성 | 모든 정산 항목의 원천 데이터 역추적 가능 |
| 멱등성 | 재실행 시 중복 정산 방지 (Unique 제약) |
| 이력 보존 | 상태 변경/취소 이력 보존 (Soft Delete) |

### 기술 스택

- **DBMS**: MySQL 8.0+ (또는 PostgreSQL 14+)
- **Charset**: utf8mb4
- **Engine**: InnoDB (트랜잭션 지원)

---

## ERD 다이어그램

### 전체 구조 (텍스트)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Upstream Domain (참조)                             │
│  ┌──────────┐    ┌──────────┐    ┌─────────────┐    ┌──────────┐           │
│  │  seller  │    │  order   │    │ order_item  │    │ payment  │           │
│  │          │◄───│          │◄───│             │    │          │           │
│  │ (1)      │    │ (N)      │    │ (N)         │    │ (1)      │           │
│  └──────────┘    └────┬─────┘    └──────┬──────┘    └────┬─────┘           │
│       │               │                 │                │                 │
│       │               └────────┬────────┘                │                 │
│       │                        │                         │                 │
│  ┌────┴────┐              ┌────┴─────┐                   │                 │
│  │         │              │  refund  │◄──────────────────┘                 │
│  │         │              │          │                                     │
│  │         │              └──────────┘                                     │
└──│─────────│────────────────────────────────────────────────────────────────┘
   │         │
   │         │
┌──│─────────│────────────────────────────────────────────────────────────────┐
│  │         │            Settlement Domain (정산 전용)                        │
│  │         │                                                                │
│  │    ┌────┴──────────────────────────────────────────────┐                │
│  │    │                                                    │                │
│  │    ▼                                                    ▼                │
│  │  ┌──────────────┐                         ┌─────────────────────────┐   │
│  │  │  settlement  │────────────────────────▶│   settlement_item       │   │
│  │  │              │ (1)              (N)    │                         │   │
│  │  │              │                         │  source_id ──────────────┼───┼──▶ order_item.id
│  │  └──────────────┘                         │             ──────────────┼───┼──▶ refund.id
│  │         │                                 └─────────────────────────┘   │
│  │         │                                                                │
│  └─────────┼────────────────────────────────────────────────────────────────┘
             │
             │
┌────────────│────────────────────────────────────────────────────────────────┐
│            │                    Batch Domain                                 │
│            │                                                                │
│            │           ┌───────────────────────────┐                        │
│            └──────────▶│ settlement_job_execution  │                        │
│          (논리적 참조)   │                           │                        │
│                        └───────────────────────────┘                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 관계 요약

| 관계 | 설명 |
|------|------|
| seller (1) : order (N) | 한 판매자가 여러 주문을 받음 |
| order (1) : order_item (N) | 한 주문에 여러 상품이 포함됨 |
| order (1) : payment (1) | 한 주문에 하나의 결제 (단순화) |
| order_item (1) : refund (0..N) | 한 주문상품에 여러 환불 가능 (부분 환불) |
| seller (1) : settlement (N) | 한 판매자가 여러 정산을 받음 (기간별) |
| settlement (1) : settlement_item (N) | 한 정산에 여러 항목이 포함됨 |
| settlement_item → order_item | SALE 항목의 원천 (FK 아닌 논리 참조) |
| settlement_item → refund | REFUND 항목의 원천 (FK 아닌 논리 참조) |

---

## 테이블 상세 설계

### 1. seller (판매자)

#### 역할

판매자(Seller) 기본 정보를 관리한다. 정산의 **주체**가 되는 엔티티.

#### DDL

```sql
-- MySQL 8.0
CREATE TABLE seller (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    seller_code     VARCHAR(20)     NOT NULL,
    seller_name     VARCHAR(100)    NOT NULL,
    business_number VARCHAR(12)     NULL,
    email           VARCHAR(100)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    commission_rate DECIMAL(5,4)    NOT NULL DEFAULT 0.1000,
    settlement_cycle VARCHAR(20)    NOT NULL DEFAULT 'DAILY',
    bank_code       VARCHAR(10)     NULL,
    account_number  VARCHAR(30)     NULL,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_seller_code (seller_code),
    INDEX idx_seller_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 주요 컬럼

| 컬럼 | 타입 | 역할 |
|------|------|------|
| status | VARCHAR(20) | 판매자 상태 (ACTIVE, SUSPENDED, WITHDRAWN) |
| commission_rate | DECIMAL(5,4) | 개별 수수료율 (0.1000 = 10%) |
| settlement_cycle | VARCHAR(20) | 정산 주기 (DAILY, WEEKLY) |
| bank_code, account_number | VARCHAR | 정산금 입금 계좌 정보 |

#### 정산 관점 포인트

- `status = 'ACTIVE'`인 판매자만 정산 대상
- `commission_rate`는 판매자별 차등 수수료 적용 시 사용
- `settlement_cycle`로 일별/주별 정산 구분

---

### 2. order (주문)

#### 역할

고객의 주문 정보를 관리한다. 정산 시 **판매자 식별**과 **주문 상태 확인**에 사용.

#### DDL

```sql
CREATE TABLE `order` (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_number    VARCHAR(30)     NOT NULL,
    seller_id       BIGINT          NOT NULL,
    buyer_id        BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(15,2)   NOT NULL,
    ordered_at      DATETIME(6)     NOT NULL,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_order_number (order_number),
    INDEX idx_order_seller (seller_id),
    INDEX idx_order_status (status),
    INDEX idx_order_ordered_at (ordered_at),

    CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES seller(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 주요 컬럼

| 컬럼 | 타입 | 역할 |
|------|------|------|
| status | VARCHAR(20) | 주문 상태 (PENDING, PAID, SHIPPED, DELIVERED, COMPLETED, CANCELED) |
| seller_id | BIGINT | 주문 귀속 판매자 (정산 대상 식별) |
| ordered_at | DATETIME(6) | 주문 시각 (정산 기준 시점 아님, 참고용) |

#### 인덱스 설계 의도

- `idx_order_seller`: 판매자별 주문 조회 (정산 대상 집계)
- `idx_order_ordered_at`: 기간별 주문 조회

---

### 3. order_item (주문 상품)

#### 역할

주문에 포함된 개별 상품 정보. **정산 금액 계산의 기본 단위**.

#### DDL

```sql
CREATE TABLE order_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL,
    product_id      BIGINT          NOT NULL,
    product_name    VARCHAR(200)    NOT NULL,
    quantity        INT             NOT NULL,
    unit_price      DECIMAL(15,2)   NOT NULL,
    total_price     DECIMAL(15,2)   NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_order_item_order (order_id),
    INDEX idx_order_item_product (product_id),

    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES `order`(id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_item_price CHECK (unit_price >= 0 AND total_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 주요 컬럼

| 컬럼 | 타입 | 역할 |
|------|------|------|
| unit_price | DECIMAL(15,2) | 상품 단가 |
| total_price | DECIMAL(15,2) | 합계 (unit_price × quantity) |
| status | VARCHAR(20) | 상품 상태 (ACTIVE, CANCELED) |

#### 정산 관점 포인트

- `total_price`가 SALE 항목의 `gross_amount`가 됨
- `status = 'CANCELED'`인 항목은 정산 제외
- settlement_item에서 `source_id`로 참조됨

---

### 4. payment (결제)

#### 역할

주문에 대한 결제 정보. **정산 기준 시점(paidAt)**을 제공.

#### DDL

```sql
CREATE TABLE payment (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    payment_key     VARCHAR(50)     NOT NULL,
    order_id        BIGINT          NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    payment_method  VARCHAR(30)     NOT NULL,
    paid_at         DATETIME(6)     NULL,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_key (payment_key),
    UNIQUE KEY uk_payment_order (order_id),
    INDEX idx_payment_status (status),
    INDEX idx_payment_paid_at (paid_at),

    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES `order`(id),
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 주요 컬럼

| 컬럼 | 타입 | 역할 |
|------|------|------|
| status | VARCHAR(20) | 결제 상태 (PENDING, CONFIRMED, FAILED, CANCELED) |
| paid_at | DATETIME(6) | **정산 기준 시점** (결제 완료 시각) |

#### 정산 관점 포인트

- `status = 'CONFIRMED'`이고 `paid_at`이 정산 기간 내인 건만 정산 대상
- `paid_at`에 인덱스 필수 (정산 대상 조회 성능)

```sql
-- 정산 대상 조회 시 사용되는 쿼리 패턴
SELECT oi.*
FROM order_item oi
JOIN `order` o ON oi.order_id = o.id
JOIN payment p ON o.id = p.order_id
WHERE o.seller_id = ?
  AND p.status = 'CONFIRMED'
  AND p.paid_at >= ?  -- periodStart
  AND p.paid_at < ?   -- periodEnd + 1 day
```

---

### 5. refund (환불)

#### 역할

환불 정보 관리. **정산 차감(REFUND 항목)**의 원천.

#### DDL

```sql
CREATE TABLE refund (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    refund_key      VARCHAR(50)     NOT NULL,
    order_item_id   BIGINT          NOT NULL,
    refund_type     VARCHAR(20)     NOT NULL,
    refund_amount   DECIMAL(15,2)   NOT NULL,
    refund_quantity INT             NULL,
    reason          VARCHAR(500)    NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    refunded_at     DATETIME(6)     NULL,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_key (refund_key),
    INDEX idx_refund_order_item (order_item_id),
    INDEX idx_refund_status (status),
    INDEX idx_refund_refunded_at (refunded_at),

    CONSTRAINT fk_refund_order_item FOREIGN KEY (order_item_id) REFERENCES order_item(id),
    CONSTRAINT chk_refund_amount CHECK (refund_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 주요 컬럼

| 컬럼 | 타입 | 역할 |
|------|------|------|
| refund_type | VARCHAR(20) | 환불 유형 (FULL, PARTIAL_AMOUNT, PARTIAL_QUANTITY) |
| refund_amount | DECIMAL(15,2) | 환불 금액 |
| refund_quantity | INT | 부분 환불 시 환불 수량 |
| status | VARCHAR(20) | 환불 상태 (PENDING, COMPLETED, REJECTED) |
| refunded_at | DATETIME(6) | **환불 완료 시각** (정산 반영 기준) |

#### 정산 관점 포인트

- `status = 'COMPLETED'`이고 `refunded_at`이 정산 기간 내인 건을 REFUND 항목으로 생성
- settlement_item에서 `source_id`로 참조됨

---

### 6. settlement (정산)

#### 역할

판매자별 정산 결과 스냅샷. **정산 도메인의 핵심 Aggregate Root**.

#### DDL

```sql
CREATE TABLE settlement (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    seller_id           BIGINT          NOT NULL,
    cycle_type          VARCHAR(20)     NOT NULL,
    period_start        DATE            NOT NULL,
    period_end          DATE            NOT NULL,

    -- 금액 필드
    gross_sales_amount  DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    refund_amount       DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    commission_amount   DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    tax_amount          DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    adjustment_amount   DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    payout_amount       DECIMAL(15,2)   NOT NULL DEFAULT 0.00,

    -- 상태/이력 필드
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    re_settled_from     BIGINT          NULL,
    cancel_reason       VARCHAR(500)    NULL,

    -- 타임스탬프
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at        DATETIME(6)     NULL,
    paid_at             DATETIME(6)     NULL,
    canceled_at         DATETIME(6)     NULL,
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_settlement_seller (seller_id),
    INDEX idx_settlement_period (period_start, period_end),
    INDEX idx_settlement_status (status),

    CONSTRAINT fk_settlement_seller FOREIGN KEY (seller_id) REFERENCES seller(id),
    CONSTRAINT chk_settlement_period CHECK (period_start <= period_end),
    CONSTRAINT chk_settlement_amounts CHECK (
        gross_sales_amount >= 0 AND
        refund_amount >= 0 AND
        commission_amount >= 0 AND
        tax_amount >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 멱등성 보장을 위한 Partial Unique Index
-- MySQL 8.0: 함수 기반 유니크 인덱스로 구현
ALTER TABLE settlement
ADD COLUMN status_active_flag TINYINT
    GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING', 'CONFIRMED', 'PAID') THEN 1 ELSE NULL END) STORED;

CREATE UNIQUE INDEX uk_settlement_active
ON settlement (seller_id, cycle_type, period_start, period_end, status_active_flag);
```

#### 주요 컬럼

| 컬럼 | 타입 | 역할 |
|------|------|------|
| cycle_type | VARCHAR(20) | 정산 주기 (DAILY, WEEKLY) |
| period_start/end | DATE | 정산 대상 기간 |
| gross_sales_amount | DECIMAL(15,2) | 총 판매금액 (SALE 합계) |
| refund_amount | DECIMAL(15,2) | 총 환불금액 (REFUND 합계) |
| commission_amount | DECIMAL(15,2) | 총 수수료 |
| tax_amount | DECIMAL(15,2) | 수수료 부가세 |
| adjustment_amount | DECIMAL(15,2) | 수동 조정금액 (+/-) |
| payout_amount | DECIMAL(15,2) | **최종 지급액** |
| status | VARCHAR(20) | 상태 (PENDING, CONFIRMED, PAID, CANCELED) |
| re_settled_from | BIGINT | 재정산 시 원본 Settlement ID |

#### Invariant (불변 조건)

```sql
-- payout_amount 계산 검증
payout_amount = gross_sales_amount
                - refund_amount
                - commission_amount
                - tax_amount
                + adjustment_amount
```

#### 핵심 제약 조건

```
동일한 (seller_id, cycle_type, period_start, period_end) 조합에서
PENDING/CONFIRMED/PAID 상태의 Settlement는 최대 1건만 존재 가능

→ 재정산 시 기존 건을 CANCELED로 변경 후 새로 생성
```

---

### 7. settlement_item (정산 항목)

#### 역할

Settlement의 개별 구성 항목. **원천 데이터와의 연결고리**.

#### DDL

```sql
CREATE TABLE settlement_item (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    settlement_id       BIGINT          NOT NULL,
    item_type           VARCHAR(20)     NOT NULL,
    source_type         VARCHAR(30)     NOT NULL,
    source_id           BIGINT          NOT NULL,

    -- 금액 필드
    gross_amount        DECIMAL(15,2)   NOT NULL,
    commission_amount   DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    tax_amount          DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    net_amount          DECIMAL(15,2)   NOT NULL,

    -- 조정 항목 전용
    description         VARCHAR(500)    NULL,

    -- 원천 데이터 시점
    source_created_at   DATETIME(6)     NOT NULL,

    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_settlement_item_settlement (settlement_id),
    INDEX idx_settlement_item_source (source_type, source_id),

    -- 동일 정산 내 원천 데이터 중복 방지
    UNIQUE KEY uk_settlement_item_source (settlement_id, source_type, source_id),

    CONSTRAINT fk_settlement_item_settlement FOREIGN KEY (settlement_id)
        REFERENCES settlement(id) ON DELETE CASCADE,
    CONSTRAINT chk_settlement_item_type CHECK (item_type IN ('SALE', 'REFUND', 'ADJUSTMENT')),
    CONSTRAINT chk_settlement_item_source CHECK (source_type IN ('ORDER_ITEM', 'REFUND', 'MANUAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 주요 컬럼

| 컬럼 | 타입 | 역할 |
|------|------|------|
| item_type | VARCHAR(20) | 항목 유형 (SALE, REFUND, ADJUSTMENT) |
| source_type | VARCHAR(30) | 원천 데이터 타입 (ORDER_ITEM, REFUND, MANUAL) |
| source_id | BIGINT | 원천 데이터 ID (order_item.id 또는 refund.id) |
| gross_amount | DECIMAL(15,2) | 항목 총액 |
| commission_amount | DECIMAL(15,2) | 항목별 수수료 |
| net_amount | DECIMAL(15,2) | 항목별 순액 |
| description | VARCHAR(500) | ADJUSTMENT 시 사유 (필수) |

#### 유형별 데이터 예시

| item_type | source_type | source_id | 설명 |
|-----------|-------------|-----------|------|
| SALE | ORDER_ITEM | 12345 | 주문상품 12345의 판매 |
| REFUND | REFUND | 67890 | 환불 67890의 차감 |
| ADJUSTMENT | MANUAL | NULL 또는 0 | 수동 조정 |

#### 핵심 제약 조건

```
동일 Settlement 내에서 (source_type, source_id) 조합은 유일해야 함

→ 같은 OrderItem이 한 정산에 두 번 들어가는 것 방지
→ 재정산 시 기존 Settlement가 CANCELED되므로 새 Settlement에서 다시 포함 가능
```

---

### 8. settlement_job_execution (정산 배치 실행)

#### 역할

배치 Job 실행 이력 관리. **멱등성 보장**과 **장애 복구**의 핵심.

#### DDL

```sql
CREATE TABLE settlement_job_execution (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    job_name            VARCHAR(50)     NOT NULL,
    cycle_type          VARCHAR(20)     NOT NULL,
    target_date         DATE            NOT NULL,
    period_start        DATE            NOT NULL,
    period_end          DATE            NOT NULL,

    -- 실행 결과
    status              VARCHAR(20)     NOT NULL DEFAULT 'RUNNING',
    total_seller_count  INT             NOT NULL DEFAULT 0,
    success_count       INT             NOT NULL DEFAULT 0,
    fail_count          INT             NOT NULL DEFAULT 0,
    skip_count          INT             NOT NULL DEFAULT 0,

    -- 메타 정보
    triggered_by        VARCHAR(50)     NULL,
    parent_job_id       BIGINT          NULL,
    error_message       TEXT            NULL,

    -- 타임스탬프
    started_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at        DATETIME(6)     NULL,

    PRIMARY KEY (id),
    INDEX idx_job_execution_target (target_date, cycle_type),
    INDEX idx_job_execution_status (status),

    CONSTRAINT fk_job_execution_parent FOREIGN KEY (parent_job_id)
        REFERENCES settlement_job_execution(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 동일 기간 중복 실행 방지 (COMPLETED 상태에 대해서만)
-- MySQL 8.0: 함수 기반
ALTER TABLE settlement_job_execution
ADD COLUMN status_completed_flag TINYINT
    GENERATED ALWAYS AS (CASE WHEN status = 'COMPLETED' THEN 1 ELSE NULL END) STORED;

CREATE UNIQUE INDEX uk_job_execution_completed
ON settlement_job_execution (cycle_type, period_start, period_end, status_completed_flag);
```

#### 주요 컬럼

| 컬럼 | 타입 | 역할 |
|------|------|------|
| job_name | VARCHAR(50) | Job 이름 (DailySettlementJob, ManualSettlementJob 등) |
| cycle_type | VARCHAR(20) | 정산 주기 |
| target_date | DATE | 배치 대상 일자 (실행일 기준) |
| period_start/end | DATE | 정산 대상 기간 |
| status | VARCHAR(20) | 실행 상태 (RUNNING, COMPLETED, PARTIAL_COMPLETED, FAILED) |
| triggered_by | VARCHAR(50) | 수동 실행 시 Admin ID |
| parent_job_id | BIGINT | 재처리 시 원본 Job ID |

#### 핵심 제약 조건

```
동일한 (cycle_type, period_start, period_end) 조합에서
COMPLETED 상태의 JobExecution은 최대 1건만 존재 가능

→ 배치 시작 시 COMPLETED 존재 여부 체크하여 중복 실행 방지
```

---

## 정산 멱등성 제약 조건 정리

### 1. Settlement 레벨

```sql
-- Unique Key: seller_id + cycle_type + period_start + period_end + (활성 상태)
-- 구현: Partial Unique Index

SELECT COUNT(*)
FROM settlement
WHERE seller_id = ?
  AND cycle_type = ?
  AND period_start = ?
  AND period_end = ?
  AND status IN ('PENDING', 'CONFIRMED', 'PAID');

-- 결과가 > 0 이면 정산 생성 불가 (이미 존재)
```

**동작 방식**:
- 신규 정산 생성 시 위 조건 체크
- 재정산 시 기존 Settlement를 CANCELED로 변경 후 새로 INSERT

### 2. SettlementItem 레벨

```sql
-- Unique Key: settlement_id + source_type + source_id
-- 동일 정산 내 같은 원천 데이터 중복 방지

UNIQUE KEY uk_settlement_item_source (settlement_id, source_type, source_id)
```

**동작 방식**:
- 동일 OrderItem/Refund가 한 정산에 두 번 들어가는 것 방지
- INSERT 시 Duplicate Key Error로 감지

### 3. JobExecution 레벨

```sql
-- Unique Key: cycle_type + period_start + period_end + (COMPLETED 상태)
-- 동일 기간 배치 중복 실행 방지

SELECT COUNT(*)
FROM settlement_job_execution
WHERE cycle_type = ?
  AND period_start = ?
  AND period_end = ?
  AND status = 'COMPLETED';

-- 결과가 > 0 이면 배치 스킵
```

**동작 방식**:
- 배치 시작 전 COMPLETED 상태 존재 여부 체크
- 존재하면 "Already executed" 로그 후 스킵

---

## 설계 적합성 분석

### 정합성 관점

| 설계 요소 | 효과 |
|-----------|------|
| DECIMAL(15,2) 사용 | 부동소수점 오차 방지, 금액 정확도 보장 |
| CHECK 제약조건 | DB 레벨에서 금액 음수/기간 역전 방지 |
| 금액 필드 분리 | gross/commission/tax/net 분리로 계산 추적 가능 |
| Invariant 검증 | payout = gross - refund - commission - tax + adjustment 검증 |

### 추적성 관점

| 설계 요소 | 효과 |
|-----------|------|
| source_type + source_id | 정산 항목 → 원천 데이터 역추적 가능 |
| re_settled_from | 재정산 시 원본 Settlement 추적 |
| parent_job_id | 재처리 배치 → 원본 배치 추적 |
| 타임스탬프 분리 | created/confirmed/paid/canceled 각각 기록 |

### 재실행 관점

| 설계 요소 | 효과 |
|-----------|------|
| Partial Unique Index | 활성 상태에서만 유니크 → 취소 후 재생성 가능 |
| status 기반 제약 | CANCELED 상태는 유니크 제약에서 제외 |
| cancel_reason 기록 | 재정산 사유 추적 |
| Soft Delete | 물리 삭제 없이 이력 보존 |

### 성능 관점

| 설계 요소 | 효과 |
|-----------|------|
| paid_at 인덱스 | 정산 대상 조회 성능 최적화 |
| Composite 인덱스 | (seller_id, period_start, period_end) 정산 조회 |
| FK 최소화 | settlement_item → order_item은 논리 참조 (FK 없음) |

```sql
-- 정산 대상 조회 Explain 예상
EXPLAIN
SELECT ...
FROM order_item oi
JOIN `order` o ON oi.order_id = o.id
JOIN payment p ON o.id = p.order_id
WHERE o.seller_id = 12345
  AND p.paid_at >= '2025-01-15 00:00:00'
  AND p.paid_at < '2025-01-16 00:00:00';

-- 예상 실행 계획:
-- 1. idx_payment_paid_at으로 payment 범위 스캔
-- 2. order 조인 (PK)
-- 3. idx_order_seller로 seller_id 필터
-- 4. order_item 조인
```

---

## 마이그레이션 고려사항

### 초기 데이터

```sql
-- 판매자 수수료율 기본값 설정
UPDATE seller SET commission_rate = 0.1000 WHERE commission_rate IS NULL;

-- 기존 주문 데이터 정합성 체크
SELECT o.id, o.total_amount, SUM(oi.total_price) AS item_sum
FROM `order` o
JOIN order_item oi ON o.id = oi.order_id
GROUP BY o.id
HAVING o.total_amount != item_sum;
```

### 인덱스 생성 순서

```sql
-- 대용량 테이블 인덱스는 서비스 영향 최소화를 위해 ALGORITHM=INPLACE 사용
ALTER TABLE payment
ADD INDEX idx_payment_paid_at (paid_at),
ALGORITHM=INPLACE, LOCK=NONE;
```

---

## 관련 문서

- [[도메인 정의서]]
- [[정산 규칙 및 정책]]
- [[유스케이스 명세서]]
- [[사이드 프로젝트 공통 가이드]]
