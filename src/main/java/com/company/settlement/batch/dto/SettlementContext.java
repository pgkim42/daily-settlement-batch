package com.company.settlement.batch.dto;

import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.domain.entity.SettlementItem;
import com.company.settlement.domain.enums.SettlementItemType;

import java.util.List;

/**
 * Processor -> Writer 간 데이터 전달용 DTO
 * Settlement와 SettlementItem 목록을 함께 전달
 */
public record SettlementContext(
    Settlement settlement,
    List<SettlementItem> items
) {

    /**
     * Settlement와 SettlementItem 연결
     * Writer 호출 전에 실행하여 양방향 관계 설정
     */
    public void linkItemsToSettlement() {
        for (SettlementItem item : items) {
            settlement.addSettlementItem(item);
        }
    }

    /**
     * 총 항목 수
     */
    public int getTotalItemCount() {
        return items.size();
    }

    /**
     * 판매 항목 수
     */
    public long getSaleItemCount() {
        return items.stream()
            .filter(item -> item.getItemType() == SettlementItemType.SALE)
            .count();
    }

    /**
     * 환불 항목 수
     */
    public long getRefundItemCount() {
        return items.stream()
            .filter(item -> item.getItemType() == SettlementItemType.REFUND)
            .count();
    }

    /**
     * 조정 항목 수
     */
    public long getAdjustmentItemCount() {
        return items.stream()
            .filter(item -> item.getItemType() == SettlementItemType.ADJUSTMENT)
            .count();
    }
}
