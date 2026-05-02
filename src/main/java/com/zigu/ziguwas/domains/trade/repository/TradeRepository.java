package com.zigu.ziguwas.domains.trade.repository;


import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // 특정 아이템과 임대인으로 거래 조회
    Optional<Trade> findByItemAndRenter(Item item, User renter);

    // 특정 아이템과 임차인으로 거래 조회
    Optional<Trade> findByItemAndRentee(Item item, User rentee);

    Optional<Trade> findByChatRoom(ChatRoom chatRoom);
}
