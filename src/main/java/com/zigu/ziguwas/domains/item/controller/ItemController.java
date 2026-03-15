package com.zigu.ziguwas.domains.item.controller;

import com.zigu.ziguwas.domains.item.api.ItemApi;
import com.zigu.ziguwas.domains.item.dto.reqdto.ItemRegisterReqDto;
import com.zigu.ziguwas.domains.item.dto.reqdto.ItemUpdateReqDto;
import com.zigu.ziguwas.domains.item.dto.resdto.ItemResDto;
import com.zigu.ziguwas.domains.item.service.ItemService;
import com.zigu.ziguwas.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemController implements ItemApi {

    private final ItemService itemService;

    /**
     * 아이템의 기본 텍스트 정보를 먼저 등록합니다.
     * @param itemRegisterReqDto 클라이언트로부터 전달받은 아이템 등록 정보 (유효성 검사 포함)
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 등록 완료된 아이템의 응답 DTO와 HTTP 201(Created) 상태 코드
     */
    @PostMapping
    public ResponseEntity<ItemResDto> registerItem(
            @RequestBody @Valid ItemRegisterReqDto itemRegisterReqDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.getUserId();

        ItemResDto itemResDto = itemService.registerItem(itemRegisterReqDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(itemResDto);
    }

    /**
     * 생성된 아이템에 실제 이미지 파일들을 업로드합니다.
     * @param itemId 대상 아이템 ID
     * @param images 멀티파트 파일 리스트
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 이미지가 추가된 최종 결과
     */
    @PostMapping(value = "/{itemId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResDto> uploadItemImages(
            @PathVariable("itemId") Long itemId,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        ItemResDto response = itemService.uploadImages(itemId, images, customUserDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 아이템 이미지를 삭제합니다.
     * @param itemId 대상 아이템 ID
     * @param imageId 대상 아이템 이미지 ID
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 이미지 삭제 결과
     */
    @DeleteMapping("/{itemId}/images/{imageId}")
    public ResponseEntity<String> deleteItemImages(
            @PathVariable("itemId") Long itemId,
            @PathVariable("imageId") Long imageId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        itemService.deleteImage(itemId, imageId, customUserDetails.getUserId());
        return ResponseEntity.ok("삭제 완료.");
    }

    /**
     * 아이템 등록 글을 수정 합니다.
     * @param itemId 대상 아이템 ID
     * @param itemUpdateReqDto 수정할 정보
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 수정된 정보
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<ItemResDto> updateItem(
            @PathVariable("itemId") Long itemId,
            @RequestBody @Valid ItemUpdateReqDto itemUpdateReqDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        ItemResDto response = itemService.updateItem(itemId, itemUpdateReqDto, customUserDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 아이템 등록 글을 삭제합니다.
     * @param itemId 대상 아이템 ID
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<String> deleteItem(
            @PathVariable("itemId") Long itemId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        itemService.deleteItem(itemId, customUserDetails.getUserId());
        return ResponseEntity.ok("삭제 완료.");
    }

    /**
     * 아이템 글을 상세조회 합니다.
     * @param itemId 해당 item 조회할 ID
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 조회 할 정보
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResDto> getItemDetail(
            @PathVariable("itemId") Long itemId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        ItemResDto response = itemService.getItemDetail(itemId, customUserDetails.getUserId());
        return ResponseEntity.ok(response);
    }


}

