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


    /**
     * 거래 ID와 사용자(임대인)를 조건으로 빌려줄(빌려준) 거래를 조회합니다.
     *
     * @param tradeId 거래 ID
     * @param renter 빌려준(줄) 사람
     * @return 필터링된 거래 정보 Optional
     */
    @EntityGraph(attributePaths = {"item", "rentee"})
    Optional<Trade> findByIdAndRenter(Long tradeId, User renter);
}
