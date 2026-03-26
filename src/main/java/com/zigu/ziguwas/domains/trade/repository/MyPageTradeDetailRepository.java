package com.zigu.ziguwas.domains.trade.repository;

import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MyPageTradeDetailRepository extends JpaRepository<Trade, Long> {

    /**
     * 거래 ID, 사용자(임차인), 그리고 특정 상태를 조건으로 빌린 거래를 조회합니다.
     * @param tradeId 거래 ID
     * @param rentee 빌린 사람
     * @return 필터링된 거래 정보 Optional
     */
    @EntityGraph(attributePaths = {"item", "renter"})
    Optional<Trade> findByIdAndRentee(Long tradeId, User rentee);
}
