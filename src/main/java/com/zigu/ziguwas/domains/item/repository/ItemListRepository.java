package com.zigu.ziguwas.domains.item.repository;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ItemListRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /**
     * 특정 유저가 등록한 아이템 중 상태가 'REGISTERED'인 것들을 최신순으로 전체 조회합니다.
     * MyPageService 에서 사용
     *
     * @param userId 사용자 식별자
     * @param status 아이템 상태 (예: ItemStatus.REGISTERED)
     * @return 해당 조건에 맞는 아이템 리스트
     */
    List<Item> findAllByUserIdAndItemStatusOrderByCreatedAtDesc(Long userId, ItemStatus status);

}
