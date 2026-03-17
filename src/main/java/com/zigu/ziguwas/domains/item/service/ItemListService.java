package com.zigu.ziguwas.domains.item.service;

import com.zigu.ziguwas.domains.item.dto.response.ItemListResDto;
import com.zigu.ziguwas.domains.item.dto.response.ItemSearchCond;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemListRepository;
import com.zigu.ziguwas.domains.item.repository.spec.ItemSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemListService {

    private final ItemListRepository itemListRepository;

    public Page<ItemListResDto> getItemList(ItemSearchCond cond, Pageable pageable , Long userId) {

        // 1. 정렬 조건 결정
        Sort sort = Sort.by(Sort.Direction.DESC, "id"); // 기본값: 최신순

        if ("인기순".equals(cond.getSort())) {
            sort = Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "id"));
        } else if ("낮은가격순".equals(cond.getSort())) {
            sort = Sort.by(Sort.Direction.ASC, "dayPerPrice").and(Sort.by(Sort.Direction.DESC, "id"));
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // 2. 검색 조건 조합 및 조회
        Specification<Item> spec = ItemSpecs.withCategory(cond.getCategory());
        // 카테고리조건 + 정렬
        Page<Item> items = itemListRepository.findAll(spec, sortedPageable);

        return items.map(ItemListResDto::fromEntity);
    }
}
