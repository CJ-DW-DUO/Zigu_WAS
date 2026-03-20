package com.zigu.ziguwas.domains.trade.repository;

import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MyPageTradeRepository extends JpaRepository<Trade, Long> {

    /**
     * 내가 빌린 물건 조회
     *
     * @param rentee 빌린 물건을 조회하기위해
     * @param status 특정 상태 조회
     */
    @EntityGraph(attributePaths = {"item", "renter"})
    List<Trade> findAllByRenteeAndTradeStatus(User rentee, TradeStatus status);

    /**
     * 내가 빌려준 모든 물건 조회
     *
     * @param renter 빌려준사람으로 조회 하기위해
     */
    @EntityGraph(attributePaths = {"item", "rentee"})
    List<Trade> findAllByRenter(User renter);
}
