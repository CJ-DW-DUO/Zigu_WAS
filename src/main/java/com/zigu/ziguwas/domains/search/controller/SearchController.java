package com.zigu.ziguwas.domains.search.controller;

import com.zigu.ziguwas.domains.search.api.SearchApi;
import com.zigu.ziguwas.domains.search.dto.request.ItemSearchReqDto;
import com.zigu.ziguwas.domains.search.dto.response.ItemSearchResDto;
import com.zigu.ziguwas.domains.search.dto.response.SearchHistoryResDto;
import com.zigu.ziguwas.domains.search.service.SearchService;
import com.zigu.ziguwas.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController implements SearchApi {

    private final SearchService searchService;

    @GetMapping("/item")
    public ResponseEntity<Page<ItemSearchResDto>> searchItems(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ItemSearchReqDto reqDto,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        return ResponseEntity.ok(searchService.searchItems(userDetails.getUserId(), reqDto, pageable));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SearchHistoryResDto>> getSearchHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(searchService.getSearchHistory(userDetails.getUserId()));
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        searchService.clearSearchHistory(userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/history/{searchId}") // 개별 삭제용 경로
    public ResponseEntity<Void> deleteHistoryItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long searchId
    ) {

        searchService.deleteSearchHistoryById(userDetails.getUserId(), searchId);
        return ResponseEntity.ok().build();
    }
}