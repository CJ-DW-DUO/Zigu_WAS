package com.zigu.ziguwas.domains.item.repository;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findTop3ByUserOrderByCreatedAtDesc(User user);

    /**
     * 키워드로 아이템을 페이징 검색합니다.
     *
     * @param keyword 검색어
     * @param pageable 페이징 및 정렬 객체
     * @return 페이징 처리된 아이템 목록
     */
    Page<Item> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * 키워드와 카테고리로 아이템을 페이징 검색합니다.
     *
     * @param keyword 검색어
     * @param category 카테고리
     * @param pageable 페이징 및 정렬 객체
     * @return 페이징 처리된 아이템 목록
     */
    Page<Item> findByTitleContainingIgnoreCaseAndCategory(String keyword, ItemCategory category, Pageable pageable);
}
