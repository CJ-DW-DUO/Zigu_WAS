package com.zigu.ziguwas.domains.item.repository;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {
    /**
     * 특정 아이템의 이미지중 가장 먼저 등록된 하나를 가져옵니다.
     * @param item item 엔티티
     */
    Optional<ItemImage> findFirstByItemOrderByImageIdAsc(Item item);

}
