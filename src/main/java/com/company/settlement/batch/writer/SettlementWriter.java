package com.company.settlement.batch.writer;

import com.company.settlement.batch.dto.SettlementContext;
import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.repository.SettlementRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 정산 데이터 저장 Writer
 *
 * 청크 단위로 Settlement 및 SettlementItem 저장
 * - CascadeType.ALL로 SettlementItem 자동 저장
 * - EntityManager flush/clear로 메모리 최적화
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementWriter implements ItemWriter<SettlementContext> {

    private final SettlementRepository settlementRepository;
    private final EntityManager entityManager;

    /**
     * 청크 단위로 Settlement 및 SettlementItem 저장
     *
     * @param chunk Processor에서 전달된 SettlementContext 목록
     *
     * 트랜잭션: Step의 transactionManager에 의해 청크 단위로 관리됨
     * - 청크 내 하나라도 실패 시 전체 롤백
     * - Skip 설정으로 개별 실패는 다음 청크에서 계속 처리
     */
    @Override
    public void write(Chunk<? extends SettlementContext> chunk) throws Exception {
        log.info("[SettlementWriter] Writing {} settlements", chunk.size());

        List<Settlement> settlementsToSave = new ArrayList<>();

        for (SettlementContext context : chunk) {
            // Settlement와 SettlementItem 연결
            context.linkItemsToSettlement();

            Settlement settlement = context.settlement();
            settlementsToSave.add(settlement);

            log.debug("[SettlementWriter] Prepared settlement: seller={}, payout={}, saleItems={}, refundItems={}",
                      settlement.getSeller().getSellerCode(),
                      settlement.getPayoutAmount(),
                      context.getSaleItemCount(),
                      context.getRefundItemCount());
        }

        // Batch Insert (CascadeType.ALL로 SettlementItem도 함께 저장)
        List<Settlement> savedSettlements = settlementRepository.saveAll(settlementsToSave);

        // JPA 배치 최적화를 위한 flush & clear
        entityManager.flush();
        entityManager.clear();  // 영속성 컨텍스트 정리 (메모리 최적화)

        log.info("[SettlementWriter] Successfully saved {} settlements", savedSettlements.size());

        // 저장 결과 로깅
        for (Settlement saved : savedSettlements) {
            log.info("[SettlementWriter] Saved: id={}, seller={}, period={} ~ {}, grossSales={}, payout={}",
                     saved.getId(),
                     saved.getSeller().getSellerCode(),
                     saved.getPeriodStart(),
                     saved.getPeriodEnd(),
                     saved.getGrossSalesAmount(),
                     saved.getPayoutAmount());
        }
    }
}
