package com.zigu.ziguwas.domains.item.repository;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    List<Item> findTop3ByUserOrderByCreatedAtDesc(User user);

    /**
     * 조회수만 별도의 짧은 트랜잭션(REQUIRES_NEW)으로 증가시킵니다.
     * 상세조회 트랜잭션과 분리해 row lock 보유 시간을 최소화하여,
     * 동시 조회 시 락 경합으로 상세조회 응답이 지연되는 것을 방지합니다.
     */
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE Item i SET i.viewCount = i.viewCount + 1 WHERE i.id = :itemId")
    void increaseViewCount(@Param("itemId") Long itemId);

    /**
     * 대여 승인 시 같은 아이템에 대한 동시 승인 요청이 겹치는 기간을 이중으로
     * 통과시키지 않도록, row 락을 걸고 아이템을 조회합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :itemId")
    Optional<Item> findByIdForUpdate(@Param("itemId") Long itemId);
}
