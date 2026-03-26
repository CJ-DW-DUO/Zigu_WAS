package com.zigu.ziguwas.domains.trade.repository;


import com.zigu.ziguwas.domains.trade.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
