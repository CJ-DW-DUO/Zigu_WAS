package com.zigu.ziguwas.domains.item.controller;

import com.zigu.ziguwas.domains.item.api.ItemListApi;
import com.zigu.ziguwas.domains.item.dto.response.ItemListResDto;
import com.zigu.ziguwas.domains.item.dto.response.ItemSearchCond;
import com.zigu.ziguwas.domains.item.service.ItemListService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemListController implements ItemListApi {

    private final ItemListService itemListService;

    /**
     * 아이템 목록을 필터링 및 정렬 조건에 따라 페이징 조회합니다.
     *
     * @param cond     카테고리 및 정렬 조건 (category, sort)
     * @param pageable 페이징 정보 (page, size)
     * @return 페이징된 아이템 목록 응답
     */
    @GetMapping
    public ResponseEntity<Page<ItemListResDto>> getItemList(
            @ModelAttribute ItemSearchCond cond,
            @PageableDefault(size = 12) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {

        return ResponseEntity.ok(itemListService.getItemList(cond, pageable , customUserDetails.getUserId()));
    }
}
