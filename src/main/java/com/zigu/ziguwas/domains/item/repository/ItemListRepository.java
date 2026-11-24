package com.zigu.ziguwas.domains.item.repository;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemListRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /**
     * 특정 유저가 등록한 아이템을 상태와 무관하게 최신순으로 전체 조회합니다.
     * MyPageService 에서 사용
     *
     * @param userId 사용자 식별자
     * @return 해당 유저의 아이템 리스트
     */
    List<Item> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 유저가 등록한 아이템 개수를 조회합니다.
     *
     * @param userId 사용자 식별자
     */
    long countByUserId(Long userId);

    /**
     * 특정 유저가 등록한 아이템 중 주어진 상태의 거래가 걸려 있지 않은 것만 조회합니다.
     * 회원 탈퇴 때 지워도 되는 물건만 골라 오는 용도입니다. 거래가 걸린 물건은 아예 불러오지 않습니다.
     *
     * @param userId 사용자 식별자
     * @param statuses 삭제를 막는 거래 상태 목록 (대기 중·진행 중)
     */
    @Query("select i from Item i where i.user.id = :userId " +
            "and not exists (select t.id from Trade t where t.item = i and t.tradeStatus in :statuses) " +
            "order by i.createdAt desc")
    List<Item> findAllWithoutActiveTradeByUserId(@Param("userId") Long userId,
                                                 @Param("statuses") List<TradeStatus> statuses);

}
