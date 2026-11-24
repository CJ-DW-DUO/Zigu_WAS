package com.zigu.ziguwas.domains.item.repository;

import com.zigu.ziguwas.domains.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

}
