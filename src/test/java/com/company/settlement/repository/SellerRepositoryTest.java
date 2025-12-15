package com.company.settlement.repository;

import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.SellerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SellerRepository 통합 테스트
 */
class SellerRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private SellerRepository sellerRepository;

    @Test
    @DisplayName("판매자 코드로 조회 성공")
    void findBySellerCode_Success() {
        // given
        Seller seller = Seller.builder()
                .sellerCode("SELLER001")
                .sellerName("테스트 판매자")
                .commissionRate(new BigDecimal("0.1000"))
                .status(SellerStatus.ACTIVE)
                .build();
        sellerRepository.save(seller);

        // when
        Optional<Seller> found = sellerRepository.findBySellerCode("SELLER001");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSellerName()).isEqualTo("테스트 판매자");
        assertThat(found.get().getCommissionRate()).isEqualByComparingTo(new BigDecimal("0.1000"));
    }

    @Test
    @DisplayName("존재하지 않는 판매자 코드 조회 시 빈 Optional 반환")
    void findBySellerCode_NotFound_ReturnsEmpty() {
        // when
        Optional<Seller> found = sellerRepository.findBySellerCode("NOT_EXIST");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("활성 상태 판매자 목록 조회")
    void findByStatus_ReturnsActiveSellers() {
        // given
        Seller activeSeller1 = Seller.builder()
                .sellerCode("ACTIVE001")
                .sellerName("활성 판매자 1")
                .status(SellerStatus.ACTIVE)
                .build();
        Seller activeSeller2 = Seller.builder()
                .sellerCode("ACTIVE002")
                .sellerName("활성 판매자 2")
                .status(SellerStatus.ACTIVE)
                .build();
        Seller inactiveSeller = Seller.builder()
                .sellerCode("INACTIVE001")
                .sellerName("비활성 판매자")
                .status(SellerStatus.INACTIVE)
                .build();

        sellerRepository.saveAll(List.of(activeSeller1, activeSeller2, inactiveSeller));

        // when
        List<Seller> activeSellers = sellerRepository.findByStatus(SellerStatus.ACTIVE);

        // then
        assertThat(activeSellers).hasSize(2);
        assertThat(activeSellers).extracting(Seller::getSellerCode)
                .containsExactlyInAnyOrder("ACTIVE001", "ACTIVE002");
    }

    @Test
    @DisplayName("활성 판매자 수 집계")
    void countActiveSellers_ReturnsCorrectCount() {
        // given
        sellerRepository.save(Seller.builder()
                .sellerCode("ACTIVE001")
                .sellerName("활성 판매자 1")
                .status(SellerStatus.ACTIVE)
                .build());
        sellerRepository.save(Seller.builder()
                .sellerCode("ACTIVE002")
                .sellerName("활성 판매자 2")
                .status(SellerStatus.ACTIVE)
                .build());
        sellerRepository.save(Seller.builder()
                .sellerCode("SUSPENDED001")
                .sellerName("정지 판매자")
                .status(SellerStatus.SUSPENDED)
                .build());

        // when
        long count = sellerRepository.countActiveSellers();

        // then
        assertThat(count).isEqualTo(2);
    }
}
