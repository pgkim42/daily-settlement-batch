package com.company.settlement.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSettlementItem is a Querydsl query type for SettlementItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSettlementItem extends EntityPathBase<SettlementItem> {

    private static final long serialVersionUID = -1888935036L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSettlementItem settlementItem = new QSettlementItem("settlementItem");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<java.math.BigDecimal> commissionAmount = createNumber("commissionAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> commissionRate = createNumber("commissionRate", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<java.math.BigDecimal> grossAmount = createNumber("grossAmount", java.math.BigDecimal.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<com.company.settlement.domain.enums.SettlementItemType> itemType = createEnum("itemType", com.company.settlement.domain.enums.SettlementItemType.class);

    public final NumberPath<java.math.BigDecimal> netAmount = createNumber("netAmount", java.math.BigDecimal.class);

    public final QSettlement settlement;

    public final NumberPath<Long> sourceId = createNumber("sourceId", Long.class);

    public final EnumPath<com.company.settlement.domain.enums.SettlementSource> sourceType = createEnum("sourceType", com.company.settlement.domain.enums.SettlementSource.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSettlementItem(String variable) {
        this(SettlementItem.class, forVariable(variable), INITS);
    }

    public QSettlementItem(Path<? extends SettlementItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSettlementItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSettlementItem(PathMetadata metadata, PathInits inits) {
        this(SettlementItem.class, metadata, inits);
    }

    public QSettlementItem(Class<? extends SettlementItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.settlement = inits.isInitialized("settlement") ? new QSettlement(forProperty("settlement"), inits.get("settlement")) : null;
    }

}

