package com.company.settlement.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRefund is a Querydsl query type for Refund
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRefund extends EntityPathBase<Refund> {

    private static final long serialVersionUID = 40398624L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRefund refund = new QRefund("refund");

    public final QBaseEntity _super = new QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOrderItem orderItem;

    public final NumberPath<java.math.BigDecimal> refundAmount = createNumber("refundAmount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> refundedAt = createDateTime("refundedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> refundQuantity = createNumber("refundQuantity", Integer.class);

    public final StringPath refundReason = createString("refundReason");

    public final EnumPath<com.company.settlement.domain.enums.RefundStatus> refundStatus = createEnum("refundStatus", com.company.settlement.domain.enums.RefundStatus.class);

    public final EnumPath<com.company.settlement.domain.enums.RefundType> refundType = createEnum("refundType", com.company.settlement.domain.enums.RefundType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRefund(String variable) {
        this(Refund.class, forVariable(variable), INITS);
    }

    public QRefund(Path<? extends Refund> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRefund(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRefund(PathMetadata metadata, PathInits inits) {
        this(Refund.class, metadata, inits);
    }

    public QRefund(Class<? extends Refund> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.orderItem = inits.isInitialized("orderItem") ? new QOrderItem(forProperty("orderItem"), inits.get("orderItem")) : null;
    }

}

