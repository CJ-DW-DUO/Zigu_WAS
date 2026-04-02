package com.zigu.ziguwas.domains.user.controller;

import com.zigu.ziguwas.domains.user.api.MyPageApi;
import com.zigu.ziguwas.domains.user.dto.mypage.request.MyPageProfileReqDto;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyPageMainResDto;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyPageProfileResDto;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyRegisteredItemResDto;
import com.zigu.ziguwas.domains.user.service.MyPageService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPageController implements MyPageApi {

    private final MyPageService myPageService;

    /**
     * 마이페이지 메인 화면에 필요한 사용자 정보 및 등록 물건 요약 데이터를 조회합니다.
     * 인증된 사용자의 식별자(ID)를 기반으로 닉네임, 학교 정보, 최신 등록 물건 3건을 반환합니다.
     *
     * @param customUserDetails 인증된 사용자의 정보를 담고 있는 Principal 객체
     * @return 200 OK, 사용자 정보와 물건 리스트가 포함된 MyPageMainResDto
     */
    @GetMapping
    public ResponseEntity<MyPageMainResDto> getMyPageMain(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(myPageService.getMyPageMain(customUserDetails.getUserId()));

    }

    /**
     * 등록한 아이템을 조회합니다.
     *
     * @param customUserDetails 인증된 사용자의 정보를 담은 객체
     * @return 사용자가 등록한 물건들 list
     */
    @GetMapping("/items")
    public ResponseEntity<List<MyRegisteredItemResDto>>  getMyPageItems(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(myPageService.getMyRegisteredItems(customUserDetails.getUserId()));
    }

    /**
     * 프로필을 수정 합니다.
     * 이미지와 닉네임
     *
     * @param customUserDetails 인증된 객체 정보
     * @param reqDto 요청할 profile 수정 (닉네임)
     * @param profileImage 요청할 profile 수정 (이미지)
     * @return 수정된 DTO
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MyPageProfileResDto> updateProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestPart(value = "data") MyPageProfileReqDto reqDto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        MyPageProfileResDto response = myPageService.updateProfile(
                customUserDetails.getUserId(),
                reqDto,
                profileImage
        );
        return ResponseEntity.ok(response);
    }
}
