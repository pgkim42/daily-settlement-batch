package com.company.settlement.domain.entity;

import com.company.settlement.domain.enums.SellerStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 판매자 Entity
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sellers", indexes = {
    @Index(name = "ix_sellers_code", columnList = "sellerCode"),
    @Index(name = "ix_sellers_status", columnList = "status")
})
@Comment("판매자 정보")
public class Seller extends BaseEntity {

    @Column(name = "seller_code", nullable = false, unique = true, length = 50)
    @Comment("판매자 고유 코드")
    private String sellerCode;

    @Column(name = "seller_name", nullable = false, length = 100)
    @Comment("판매자명")
    private String sellerName;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    @Comment("수수료율 (0.1000 = 10%)")
    private BigDecimal commissionRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Comment("판매자 상태")
    private SellerStatus status;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Settlement> settlements = new ArrayList<>();

    @Builder
    public Seller(String sellerCode, String sellerName, BigDecimal commissionRate, SellerStatus status) {
        this.sellerCode = sellerCode;
        this.sellerName = sellerName;
        this.commissionRate = commissionRate != null ? commissionRate : new BigDecimal("0.1000");
        this.status = status != null ? status : SellerStatus.ACTIVE;
    }

    /**
     * 판매자 상태 변경
     */
    public void changeStatus(SellerStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 수수료율 변경
     */
    public void updateCommissionRate(BigDecimal newRate) {
        if (newRate == null || newRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("수수료율은 0 이상이어야 합니다.");
        }
        if (newRate.compareTo(new BigDecimal("1.0000")) > 0) {
            throw new IllegalArgumentException("수수료율은 100%를 초과할 수 없습니다.");
        }
        this.commissionRate = newRate;
    }

    /**
     * 정산 가능 여부 확인
     */
    public boolean isSettlementEligible() {
        return this.status == SellerStatus.ACTIVE;
    }
}