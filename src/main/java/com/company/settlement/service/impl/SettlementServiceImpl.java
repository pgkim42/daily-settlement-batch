package com.company.settlement.service.impl;

import com.company.settlement.domain.entity.Settlement;
import com.company.settlement.dto.response.SettlementDetailResponse;
import com.company.settlement.dto.response.SettlementItemResponse;
import com.company.settlement.dto.response.SettlementResponse;
import com.company.settlement.exception.SettlementAccessDeniedException;
import com.company.settlement.exception.SettlementNotFoundException;
import com.company.settlement.repository.SettlementRepository;
import com.company.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 정산 서비스 구현체 (판매자용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;

    @Override
    public Page<SettlementResponse> getMySettlements(Long sellerId, Pageable pageable) {
        log.debug("Getting settlements for seller: {}", sellerId);

        return settlementRepository.findBySellerIdWithSeller(sellerId, pageable)
            .map(SettlementResponse::from);
    }

    @Override
    public SettlementDetailResponse getSettlementDetail(Long sellerId, Long settlementId) {
        log.debug("Getting settlement detail: settlementId={}, sellerId={}", settlementId, sellerId);

        Settlement settlement = settlementRepository.findByIdWithSellerAndItems(settlementId)
            .orElseThrow(() -> new SettlementNotFoundException(settlementId));

        // 권한 체크: 본인의 정산만 조회 가능
        if (!settlement.getSeller().getId().equals(sellerId)) {
            throw new SettlementAccessDeniedException(sellerId, settlementId);
        }

        List<SettlementItemResponse> items = settlement.getSettlementItems().stream()
            .map(SettlementItemResponse::from)
            .toList();

        return SettlementDetailResponse.from(settlement, items);
    }
}
