-- 마켓플레이스 판매자 정산 시스템 테이블 생성
-- 생성일: 2025-12-08

-- 1. sellers (판매자)
CREATE TABLE sellers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_code VARCHAR(50) NOT NULL UNIQUE COMMENT '판매자 고유 코드',
    seller_name VARCHAR(100) NOT NULL COMMENT '판매자명',
    commission_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.1000 COMMENT '수수료율 (0.1000 = 10%)',
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE' COMMENT '판매자 상태',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    INDEX ix_sellers_code (seller_code),
    INDEX ix_sellers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='판매자 정보';

-- 2. orders (주문)
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(100) NOT NULL UNIQUE COMMENT '주문번호',
    seller_id BIGINT NOT NULL COMMENT '판매자 ID',
    order_status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '주문상태',
    order_date DATETIME NOT NULL COMMENT '주문일시',
    total_amount DECIMAL(15, 2) NOT NULL CHECK (total_amount >= 0) COMMENT '총 주문 금액',
    shipping_fee DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0) COMMENT '배송비',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    FOREIGN KEY (seller_id) REFERENCES sellers(id),
    INDEX ix_orders_seller (seller_id),
    INDEX ix_orders_status (order_status),
    INDEX ix_orders_date (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 정보';

-- 3. order_items (주문항목)
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    product_name VARCHAR(200) NOT NULL COMMENT '상품명',
    unit_price DECIMAL(12, 2) NOT NULL CHECK (unit_price >= 0) COMMENT '개별 상품 가격',
    quantity INT NOT NULL CHECK (quantity > 0) COMMENT '수량',
    total_amount DECIMAL(15, 2) NOT NULL CHECK (total_amount >= 0) COMMENT '상품별 총액',
    is_refunded BOOLEAN DEFAULT FALSE COMMENT '환불 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',

    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX ix_order_items_order (order_id),
    INDEX ix_order_items_refunded (is_refunded)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 상품 정보';

-- 4. payments (결제)
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE COMMENT '주문 ID (1:1 관계)',
    payment_status ENUM('PENDING', 'CONFIRMED', 'FAILED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED') NOT NULL DEFAULT 'PENDING' COMMENT '결제상태',
    payment_method VARCHAR(50) NOT NULL COMMENT '결제수단',
    payment_amount DECIMAL(15, 2) NOT NULL CHECK (payment_amount >= 0) COMMENT '결제 금액',
    paid_at DATETIME NULL COMMENT '결제 확정일시',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX ix_payments_order (order_id),
    INDEX ix_payments_status (payment_status),
    INDEX ix_payments_paid_at (paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='결제 정보';

-- 5. refunds (환불)
CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL COMMENT '주문항목 ID',
    refund_type ENUM('FULL', 'PARTIAL_AMOUNT', 'PARTIAL_QUANTITY') NOT NULL COMMENT '환불 유형',
    refund_amount DECIMAL(15, 2) NOT NULL CHECK (refund_amount >= 0) COMMENT '환불 금액',
    refund_quantity INT NOT NULL DEFAULT 0 CHECK (refund_quantity >= 0) COMMENT '환불 수량',
    refund_reason VARCHAR(500) COMMENT '환불 사유',
    refund_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED') NOT NULL DEFAULT 'PENDING' COMMENT '환불 상태',
    refunded_at DATETIME NULL COMMENT '환불 완료일시',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',

    FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    INDEX ix_refunds_order_item (order_item_id),
    INDEX ix_refunds_status (refund_status),
    INDEX ix_refunds_refunded_at (refunded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='환불 정보';

-- 6. settlements (정산)
CREATE TABLE settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL COMMENT '판매자 ID',
    cycle_type ENUM('DAILY', 'WEEKLY') NOT NULL COMMENT '정산 주기',
    period_start DATE NOT NULL COMMENT '정산 기간 시작일',
    period_end DATE NOT NULL COMMENT '정산 기간 종료일',
    gross_sales_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (gross_sales_amount >= 0) COMMENT '총 매출액',
    refund_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (refund_amount >= 0) COMMENT '환불액',
    commission_rate DECIMAL(5, 4) NOT NULL COMMENT '적용 수수료율',
    commission_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (commission_amount >= 0) COMMENT '수수료액',
    tax_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0) COMMENT '부가세(수수료의 10%)',
    adjustment_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT '조정액',
    payout_amount DECIMAL(15, 2) NOT NULL COMMENT '최종 정산액',
    status ENUM('PENDING', 'CONFIRMED', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '정산 상태',
    confirmed_at DATETIME NULL COMMENT '확정일시',
    paid_at DATETIME NULL COMMENT '지급일시',
    cancelled_at DATETIME NULL COMMENT '취소일시',
    cancel_reason VARCHAR(500) NULL COMMENT '취소 사유',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    FOREIGN KEY (seller_id) REFERENCES sellers(id),

    -- 멱등성을 위한 복합 유니크 제약조건
    UNIQUE KEY uk_settlement_period (seller_id, cycle_type, period_start, period_end),

    -- 부분 유니크 인덱스 (취소된 정산 재정산 방지)
    UNIQUE KEY uk_settlement_active (seller_id, cycle_type, period_start, period_end, status),

    INDEX ix_settlements_seller (seller_id),
    INDEX ix_settlements_status (status),
    INDEX ix_settlements_period (period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='판매자 정산 정보';

-- 7. settlement_items (정산항목)
CREATE TABLE settlement_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL COMMENT '정산 ID',
    item_type ENUM('SALE', 'REFUND', 'ADJUSTMENT') NOT NULL COMMENT '항목 타입',
    source_type ENUM('ORDER_ITEM', 'REFUND', 'MANUAL') NOT NULL COMMENT '원천 타입',
    source_id BIGINT COMMENT '원천 데이터 ID (order_item.id 또는 refund.id)',
    gross_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT '매출액/환불 원금',
    commission_rate DECIMAL(5, 4) NOT NULL COMMENT '적용 수수료율',
    commission_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (commission_amount >= 0) COMMENT '수수료액',
    net_amount DECIMAL(15, 2) NOT NULL COMMENT '순금액 (원금 - 수수료)',
    description VARCHAR(500) COMMENT '항목 설명 (특히 ADJUSTMENT 타입)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',

    FOREIGN KEY (settlement_id) REFERENCES settlements(id) ON DELETE CASCADE,

    INDEX ix_settlement_items_settlement (settlement_id),
    INDEX ix_settlement_items_type (item_type),
    INDEX ix_settlement_items_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='정산 항목 상세';

-- 8. settlement_job_executions (정산 잡 실행)
CREATE TABLE settlement_job_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL COMMENT '잡 이름 (ex: dailySettlementJob)',
    execution_date DATE NOT NULL COMMENT '실행 대상일',
    execution_status ENUM('STARTED', 'COMPLETED', 'FAILED', 'PARTIALLY_FAILED') NOT NULL DEFAULT 'STARTED' COMMENT '실행 상태',
    total_sellers INT NOT NULL DEFAULT 0 COMMENT '전체 판매자 수',
    success_count INT NOT NULL DEFAULT 0 COMMENT '성공한 판매자 수',
    failure_count INT NOT NULL DEFAULT 0 COMMENT '실패한 판매자 수',
    error_message TEXT COMMENT '전체 에러 메시지',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '시작 시간',
    completed_at TIMESTAMP NULL COMMENT '완료 시간',

    UNIQUE KEY uk_job_execution (job_name, execution_date),
    INDEX ix_job_executions_date (execution_date),
    INDEX ix_job_executions_status (execution_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='정산 배치 잡 실행 이력';

-- 테이블 생성 완료
SELECT 'All settlement tables created successfully' AS message;