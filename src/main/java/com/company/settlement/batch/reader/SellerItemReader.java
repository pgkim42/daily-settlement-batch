package com.company.settlement.batch.reader;

import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.SellerStatus;
import com.company.settlement.repository.SellerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 정산 대상 판매자 조회 Reader
 *
 * @StepScope: Step 실행 시마다 새 인스턴스 생성, Job Parameter 바인딩 가능
 */
@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class SellerItemReader implements ItemReader<Seller> {

    private final SellerRepository sellerRepository;

    private List<Seller> sellers;
    private int currentIndex = 0;

    @Value("#{jobParameters['targetDate']}")
    private String targetDateString;

    private LocalDate targetDate;

    /**
     * 초기화: 정산 대상 판매자 목록 조회
     * @StepScope로 인해 Step 시작 시 한 번만 실행됨
     */
    @PostConstruct
    public void init() {
        this.targetDate = LocalDate.parse(targetDateString);
        this.sellers = sellerRepository.findSettlementEligibleSellers(SellerStatus.ACTIVE);
        this.currentIndex = 0;

        log.info("[SellerItemReader] 정산 대상 판매자 수: {}, 정산 대상일: {}",
                 sellers.size(), targetDate);
    }

    /**
     * 판매자 한 명씩 반환
     *
     * @return 다음 판매자, 없으면 null (Step 종료)
     */
    @Override
    public Seller read() {
        if (currentIndex >= sellers.size()) {
            return null;
        }

        Seller seller = sellers.get(currentIndex);
        currentIndex++;

        log.debug("[SellerItemReader] Reading seller: {} ({})",
                  seller.getSellerName(), seller.getSellerCode());

        return seller;
    }

    /**
     * 정산 대상 날짜 반환
     */
    public LocalDate getTargetDate() {
        return this.targetDate;
    }

    /**
     * 전체 판매자 수 반환 (Listener에서 사용)
     */
    public int getTotalSellerCount() {
        return sellers != null ? sellers.size() : 0;
    }
}
