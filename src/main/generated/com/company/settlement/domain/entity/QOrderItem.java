package com.company.settlement.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderItem is a Querydsl query type for OrderItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderItem extends EntityPathBase<OrderItem> {

    private static final long serialVersionUID = 1003589337L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderItem orderItem = new QOrderItem("orderItem");

    public final QBaseEntity _super = new QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isRefunded = createBoolean("isRefunded");

    public final QOrder order;

    public final StringPath productName = createString("productName");

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final ListPath<Refund, QRefund> refunds = this.<Refund, QRefund>createList("refunds", Refund.class, QRefund.class, PathInits.DIRECT2);

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> unitPrice = createNumber("unitPrice", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QOrderItem(String variable) {
        this(OrderItem.class, forVariable(variable), INITS);
    }

    public QOrderItem(Path<? extends OrderItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderItem(PathMetadata metadata, PathInits inits) {
        this(OrderItem.class, metadata, inits);
    }

    public QOrderItem(Class<? extends OrderItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.order = inits.isInitialized("order") ? new QOrder(forProperty("order"), inits.get("order")) : null;
    }

}

