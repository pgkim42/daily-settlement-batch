package com.company.settlement.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSeller is a Querydsl query type for Seller
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSeller extends EntityPathBase<Seller> {

    private static final long serialVersionUID = 69197607L;

    public static final QSeller seller = new QSeller("seller");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<java.math.BigDecimal> commissionRate = createNumber("commissionRate", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath sellerCode = createString("sellerCode");

    public final StringPath sellerName = createString("sellerName");

    public final ListPath<Settlement, QSettlement> settlements = this.<Settlement, QSettlement>createList("settlements", Settlement.class, QSettlement.class, PathInits.DIRECT2);

    public final EnumPath<com.company.settlement.domain.enums.SellerStatus> status = createEnum("status", com.company.settlement.domain.enums.SellerStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSeller(String variable) {
        super(Seller.class, forVariable(variable));
    }

    public QSeller(Path<? extends Seller> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSeller(PathMetadata metadata) {
        super(Seller.class, metadata);
    }

}

