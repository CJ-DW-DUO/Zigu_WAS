package com.zigu.ziguwas.domains.trade.repository;


import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // 특정 아이템과 임대인으로 거래 조회
    Optional<Trade> findByItemAndRenter(Item item, User renter);

    // 특정 아이템과 임차인으로 거래 조회
    Optional<Trade> findByItemAndRentee(Item item, User rentee);

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
     * 반납 예정일이 지났는데 아직 반납 처리(IN_PROGRESS)되지 않은 거래 목록을 조회합니다.
     * 반납기한 초과 알림 스케줄러에서 사용합니다.
     */
    List<Trade> findAllByTradeStatusAndTradeEndateBefore(TradeStatus tradeStatus, LocalDate date);
}
