package com.zigu.ziguwas.domains.trade.repository;


import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // 특정 아이템에서 두 사용자 사이의 가장 최근 거래 조회 (채팅방 상대와의 거래 상태 표시용)
    Optional<Trade> findFirstByItemAndRenterIdAndRenteeIdOrderByIdDesc(Item item, Long renterId, Long renteeId);

    // 특정 아이템에서 두 사용자 사이에 주어진 상태(예: 요청/진행 중)의 거래가 있는지 확인 (채팅 목록에서 빈 방을 남길지 판단용)
    boolean existsByItemIdAndRenterIdAndRenteeIdAndTradeStatusIn(Long itemId, Long renterId, Long renteeId, List<TradeStatus> statuses);

    /**
     * 유저가 대여인(Renter) 혹은 임차인(Lessee)으로 참여 중인 거래 중
     * 특정 상태(예: 진행 중)인 거래 목록을 조회합니다.
     */
    @Query("SELECT t FROM Trade t " +
            "WHERE (t.renter.id = :userId OR t.rentee.id = :userId) " +
            "AND t.tradeStatus = :status")
    List<Trade> findAllByUserIdInvolvedAndStatus(
            @Param("userId") Long userId,
            @Param("status") TradeStatus status
    );

    boolean existsByItemAndTradeStatusIn(Item item, List<TradeStatus> statuses);

    /**
     * 특정 아이템에 대해 해당 임대인이 이미 요청/진행 중인 거래를 가지고 있는지 확인합니다.
     * (요청글에 같은 공급자가 중복 제안하는 것을 막는 용도)
     */
    boolean existsByItemAndRenterAndTradeStatusIn(Item item, User renter, List<TradeStatus> statuses);

    /**
     * 특정 아이템에 대해 해당 임차인이 이미 요청/진행 중인 거래를 가지고 있는지 확인합니다.
     * (공급글에 같은 사용자가 중복 신청하는 것을 막는 용도)
     */
    boolean existsByItemAndRenteeAndTradeStatusIn(Item item, User rentee, List<TradeStatus> statuses);

    /**
     * 특정 거래를 제외하고, 같은 아이템에 주어진 상태인 다른 거래가 있는지 확인합니다.
     * (반납 처리 시 이 거래 말고도 여전히 진행 중인 다른 예약이 있는지 확인하는 용도)
     */
    boolean existsByItemAndTradeStatusInAndIdNot(Item item, List<TradeStatus> statuses, Long id);

    /**
     * 반납 예정일이 오늘이거나 이미 지났는데 아직 반납 처리(IN_PROGRESS)되지 않은 거래 목록을 조회합니다.
     * 반납일 당일 알림과 반납기한 초과 알림 스케줄러에서 사용합니다.
     */
    List<Trade> findAllByTradeStatusAndTradeEndateLessThanEqual(TradeStatus tradeStatus, LocalDate date);

    /**
     * 주어진 기간과 겹치는(구간 교집합이 존재하는) 특정 상태의 거래가 있는지 확인합니다.
     * 겹침 조건: 기존거래.시작일 <= 요청.종료일 AND 기존거래.종료일 >= 요청.시작일
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Trade t " +
            "WHERE t.item = :item AND t.tradeStatus = :status " +
            "AND t.tradeStdate <= :endDate AND t.tradeEndate >= :startDate")
    boolean existsOverlappingTrade(
            @Param("item") Item item,
            @Param("status") TradeStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 주어진 기간과 겹치는 특정 상태의 거래 목록을 조회합니다. (대여 불가 기간 조회용)
     * 겹침 조건은 existsOverlappingTrade 와 같다.
     */
    @Query("SELECT t FROM Trade t " +
            "WHERE t.item = :item AND t.tradeStatus = :status " +
            "AND t.tradeStdate <= :endDate AND t.tradeEndate >= :startDate")
    List<Trade> findOverlappingTrades(
            @Param("item") Item item,
            @Param("status") TradeStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 주어진 기간과 겹치는 특정 상태의 거래를 잠금 읽기(FOR UPDATE)로 조회합니다.
     *
     * 승인 시 같은 아이템의 동시 승인을 막는 용도입니다. 일반 조회(existsOverlappingTrade)는
     * 트랜잭션 시작 시점의 스냅샷을 읽기 때문에, 아이템 락을 기다리는 사이 다른 승인이 커밋돼도 보지 못합니다.
     * 잠금 읽기는 항상 최신 커밋 데이터를 읽으므로 아이템 락을 얻은 뒤 호출하면 앞선 승인을 확실히 본다.
     *
     * 잠금 범위: InnoDB 는 item_id 인덱스로 훑은 그 아이템의 거래 행을 모두(+갭) 잠근다(상태·기간 조건은 훑은 뒤 거름).
     * 승인 트랜잭션이 끝날 때까지 같은 아이템의 새 신청(INSERT)이 잠깐 대기할 수 있다. 트랜잭션이 짧아 영향은 작다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trade t " +
            "WHERE t.item = :item AND t.tradeStatus = :status " +
            "AND t.tradeStdate <= :endDate AND t.tradeEndate >= :startDate")
    List<Trade> findOverlappingTradesForUpdate(
            @Param("item") Item item,
            @Param("status") TradeStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
