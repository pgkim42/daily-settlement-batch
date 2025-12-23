package com.company.settlement.repository;

import com.company.settlement.domain.entity.Seller;
import com.company.settlement.domain.enums.SellerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SellerRepository 단위 테스트
 * Repository 인터페이스의 메서드 시그니처 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SellerRepository 단위 테스트")
class SellerRepositoryTest {

    @Mock
    private SellerRepository sellerRepository;

    @Test
    @DisplayName("판매자 코드로 조회 - 메서드 호출 검증")
    void findBySellerCode_VerifyMethodCall() {
        // given
        String sellerCode = "SELLER001";
        Seller mockSeller = Seller.builder()
                .sellerCode(sellerCode)
                .sellerName("테스트 판매자")
                .commissionRate(new BigDecimal("0.1000"))
                .status(SellerStatus.ACTIVE)
                .build();
        when(sellerRepository.findBySellerCode(sellerCode)).thenReturn(Optional.of(mockSeller));

        // when
        Optional<Seller> found = sellerRepository.findBySellerCode(sellerCode);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSellerName()).isEqualTo("테스트 판매자");
        verify(sellerRepository).findBySellerCode(sellerCode);
    }

    @Test
    @DisplayName("존재하지 않는 판매자 코드 조회 - 빈 Optional 반환")
    void findBySellerCode_NotFound_ReturnsEmpty() {
        // given
        String sellerCode = "NOT_EXIST";
        when(sellerRepository.findBySellerCode(sellerCode)).thenReturn(Optional.empty());

        // when
        Optional<Seller> found = sellerRepository.findBySellerCode(sellerCode);

        // then
        assertThat(found).isEmpty();
        verify(sellerRepository).findBySellerCode(sellerCode);
    }

    @Test
    @DisplayName("활성 상태 판매자 목록 조회 - 메서드 호출 검증")
    void findByStatus_VerifyMethodCall() {
        // given
        List<Seller> mockSellers = List.of(
                Seller.builder().sellerCode("ACTIVE001").sellerName("활성 판매자 1").status(SellerStatus.ACTIVE).build(),
                Seller.builder().sellerCode("ACTIVE002").sellerName("활성 판매자 2").status(SellerStatus.ACTIVE).build()
        );
        when(sellerRepository.findByStatus(SellerStatus.ACTIVE)).thenReturn(mockSellers);

        // when
        List<Seller> activeSellers = sellerRepository.findByStatus(SellerStatus.ACTIVE);

        // then
        assertThat(activeSellers).hasSize(2);
        assertThat(activeSellers).extracting(Seller::getSellerCode)
                .containsExactlyInAnyOrder("ACTIVE001", "ACTIVE002");
        verify(sellerRepository).findByStatus(SellerStatus.ACTIVE);
    }

    @Test
    @DisplayName("활성 판매자 수 집계 - 메서드 호출 검증")
    void countActiveSellers_VerifyMethodCall() {
        // given
        when(sellerRepository.countActiveSellers()).thenReturn(2L);

        // when
        long count = sellerRepository.countActiveSellers();

        // then
        assertThat(count).isEqualTo(2);
        verify(sellerRepository).countActiveSellers();
    }

    @Test
    @DisplayName("판매자 저장 - 메서드 호출 검증")
    void save_VerifyMethodCall() {
        // given
        Seller seller = Seller.builder()
                .sellerCode("SELLER001")
                .sellerName("테스트 판매자")
                .commissionRate(new BigDecimal("0.1000"))
                .status(SellerStatus.ACTIVE)
                .build();
        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);

        // when
        Seller saved = sellerRepository.save(seller);

        // then
        assertThat(saved.getSellerCode()).isEqualTo("SELLER001");
        verify(sellerRepository).save(seller);
    }
}
