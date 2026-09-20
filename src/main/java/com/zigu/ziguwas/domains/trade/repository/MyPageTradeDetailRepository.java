package com.zigu.ziguwas.domains.trade.repository;

import com.zigu.ziguwas.domains.trade.entity.Trade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MyPageTradeDetailRepository extends JpaRepository<Trade, Long> {

    /**
     * 거래 ID로 거래를 조회하면서 매물과 임대인/임차인을 함께 가져옵니다.
     * 조회자가 거래 당사자인지는 서비스에서 검증합니다.
     *
     * @param tradeId 거래 ID
     * @return 거래 정보 Optional
     */
    @EntityGraph(attributePaths = {"item", "renter", "rentee"})
    Optional<Trade> findWithPartiesById(Long tradeId);
}
