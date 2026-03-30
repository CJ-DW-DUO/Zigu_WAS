package com.zigu.ziguwas.domains.trade.repository;

import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
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
     * @param status 특정 상태 조회
     */
    @EntityGraph(attributePaths = {"item", "rentee"})
    List<Trade> findAllByRenterAndTradeStatus(User renter, TradeStatus status);

    /**
     * 내가 보낸 대여 요청 목록을 상태별로 필터링하여 조회합니다.
     *
     * @param rentee   빌린 사람(나)
     * @param statuses 조회할 상태 목록 (예: 전체 선택 시 [REQUESTED, IN_PROGRESS, REJECTED])
     * @param pageable 페이징 및 정렬 정보
     * @return 필터링된 거래 내역 페이징 객체
     */
    @EntityGraph(attributePaths = {"item", "renter"})
    Page<Trade> findAllByRenteeAndTradeStatusIn(User rentee, Collection<TradeStatus> statuses, Pageable pageable);

    /**
     * 내가 받은 대여 요청 목록을 상태별로 필터링하여 조회합니다.
     *
     * @param renter   빌린 사람(나)
     * @param statuses 조회할 상태 목록 (예: 전체 선택 시 [REQUESTED, IN_PROGRESS, REJECTED])
     * @param pageable 페이징 및 정렬 정보
     * @return 필터링된 거래 내역 페이징 객체
     */
    @EntityGraph(attributePaths = {"item", "rentee"})
    Page<Trade> findAllByRenterAndTradeStatusIn(User renter, Collection<TradeStatus> statuses, Pageable pageable);

}
