package com.company.settlement.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSettlement is a Querydsl query type for Settlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSettlement extends EntityPathBase<Settlement> {

    private static final long serialVersionUID = 384238801L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSettlement settlement = new QSettlement("settlement");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<java.math.BigDecimal> adjustmentAmount = createNumber("adjustmentAmount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> cancelledAt = createDateTime("cancelledAt", java.time.LocalDateTime.class);

    public final StringPath cancelReason = createString("cancelReason");

    public final NumberPath<java.math.BigDecimal> commissionAmount = createNumber("commissionAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> commissionRate = createNumber("commissionRate", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> confirmedAt = createDateTime("confirmedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.company.settlement.domain.enums.CycleType> cycleType = createEnum("cycleType", com.company.settlement.domain.enums.CycleType.class);

    public final NumberPath<java.math.BigDecimal> grossSalesAmount = createNumber("grossSalesAmount", java.math.BigDecimal.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.time.LocalDateTime> paidAt = createDateTime("paidAt", java.time.LocalDateTime.class);

    public final NumberPath<java.math.BigDecimal> payoutAmount = createNumber("payoutAmount", java.math.BigDecimal.class);

    public final DatePath<java.time.LocalDate> periodEnd = createDate("periodEnd", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> periodStart = createDate("periodStart", java.time.LocalDate.class);

    public final NumberPath<java.math.BigDecimal> refundAmount = createNumber("refundAmount", java.math.BigDecimal.class);

    public final QSeller seller;

    public final ListPath<SettlementItem, QSettlementItem> settlementItems = this.<SettlementItem, QSettlementItem>createList("settlementItems", SettlementItem.class, QSettlementItem.class, PathInits.DIRECT2);

    public final EnumPath<com.company.settlement.domain.enums.SettlementStatus> status = createEnum("status", com.company.settlement.domain.enums.SettlementStatus.class);

    public final NumberPath<java.math.BigDecimal> taxAmount = createNumber("taxAmount", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> version = createNumber("version", Long.class);

    public QSettlement(String variable) {
        this(Settlement.class, forVariable(variable), INITS);
    }

    public QSettlement(Path<? extends Settlement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSettlement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSettlement(PathMetadata metadata, PathInits inits) {
        this(Settlement.class, metadata, inits);
    }

    public QSettlement(Class<? extends Settlement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.seller = inits.isInitialized("seller") ? new QSeller(forProperty("seller")) : null;
    }

}

