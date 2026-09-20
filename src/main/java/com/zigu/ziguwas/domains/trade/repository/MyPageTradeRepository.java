package com.zigu.ziguwas.domains.trade.repository;

import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 내가 먼저 보낸 요청/제안 목록을 상태별로 필터링하여 조회합니다.
     * 공급글은 임차인(나)이 보낸 대여 요청, 요청글은 임대인(나)이 보낸 대여 제안이 해당합니다.
     *
     * @param user     요청을 보낸 사람(나)
     * @param statuses 조회할 상태 목록 (예: 전체 선택 시 [REQUESTED, IN_PROGRESS, REJECTED])
     * @param pageable 페이징 및 정렬 정보
     * @return 필터링된 거래 내역 페이징 객체
     */
    @EntityGraph(attributePaths = {"item", "renter", "rentee"})
    @Query("SELECT t FROM Trade t WHERE t.tradeStatus IN :statuses AND " +
            "((t.rentee = :user AND t.proposedByRenter = false) OR (t.renter = :user AND t.proposedByRenter = true))")
    Page<Trade> findSentRequests(@Param("user") User user, @Param("statuses") Collection<TradeStatus> statuses, Pageable pageable);

    /**
     * 내가 받은 요청/제안 목록을 상태별로 필터링하여 조회합니다.
     * 공급글은 임대인(나)이 받은 대여 요청, 요청글은 임차인(나)이 받은 대여 제안이 해당합니다.
     *
     * @param user     요청을 받은 사람(나)
     * @param statuses 조회할 상태 목록 (예: 전체 선택 시 [REQUESTED, IN_PROGRESS, REJECTED])
     * @param pageable 페이징 및 정렬 정보
     * @return 필터링된 거래 내역 페이징 객체
     */
    @EntityGraph(attributePaths = {"item", "renter", "rentee"})
    @Query("SELECT t FROM Trade t WHERE t.tradeStatus IN :statuses AND " +
            "((t.renter = :user AND t.proposedByRenter = false) OR (t.rentee = :user AND t.proposedByRenter = true))")
    Page<Trade> findReceivedRequests(@Param("user") User user, @Param("statuses") Collection<TradeStatus> statuses, Pageable pageable);

}
