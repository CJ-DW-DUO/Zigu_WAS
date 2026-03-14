package com.zigu.ziguwas.domains.item.controller;

import com.zigu.ziguwas.domains.item.dto.reqdto.ItemRegisterReqDto;
import com.zigu.ziguwas.domains.item.dto.resdto.ItemResDto;
import com.zigu.ziguwas.domains.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;

    /**
     * 새로운 중고 물건을 등록합니다.
     * 클라이언트로부터 전달받은 JSON 데이터를 DTO로 매핑하여 저장 로직을 수행합니다.
     *
     * @param itemRegisterReqDto 물건 정보 및 S3 이미지 URL 리스트가 담긴 DTO
     * @param userDetails 현재 로그인한 유저의 정보 (JWT 필터에서 주입됨)
     * @return 생성된 물건의 ID와 함께 201 Created 응답 반환
     */
    @PostMapping
    public ResponseEntity<ItemResDto> registerItem(@RequestBody @Valid ItemRegisterReqDto itemRegisterReqDto , @AuthenticationPrincipal UserDetails userDetails) {
        ItemResDto itemResDto = itemService.registerItem(itemRegisterReqDto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(itemResDto);
    }
}
