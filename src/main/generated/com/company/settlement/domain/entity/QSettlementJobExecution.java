package com.company.settlement.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSettlementJobExecution is a Querydsl query type for SettlementJobExecution
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSettlementJobExecution extends EntityPathBase<SettlementJobExecution> {

    private static final long serialVersionUID = 1015356236L;

    public static final QSettlementJobExecution settlementJobExecution = new QSettlementJobExecution("settlementJobExecution");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final DateTimePath<java.time.LocalDateTime> completedAt = createDateTime("completedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath errorMessage = createString("errorMessage");

    public final DatePath<java.time.LocalDate> executionDate = createDate("executionDate", java.time.LocalDate.class);

    public final EnumPath<com.company.settlement.domain.enums.SettlementJobStatus> executionStatus = createEnum("executionStatus", com.company.settlement.domain.enums.SettlementJobStatus.class);

    public final NumberPath<Integer> failureCount = createNumber("failureCount", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath jobName = createString("jobName");

    public final DateTimePath<java.time.LocalDateTime> startedAt = createDateTime("startedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> successCount = createNumber("successCount", Integer.class);

    public final NumberPath<Integer> totalSellers = createNumber("totalSellers", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSettlementJobExecution(String variable) {
        super(SettlementJobExecution.class, forVariable(variable));
    }

    public QSettlementJobExecution(Path<? extends SettlementJobExecution> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSettlementJobExecution(PathMetadata metadata) {
        super(SettlementJobExecution.class, metadata);
    }

}

