package com.zigu.ziguwas.domains.user.service;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemStatus;
import com.zigu.ziguwas.domains.item.repository.ItemListRepository;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.user.dto.mypage.request.MyPageProfileReqDto;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyPageMainResDto;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyPageProfileResDto;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyRegisteredItemResDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemListRepository itemListRepository;
    private final S3Service s3Service;

    /**
     * 마이페이지 메인 화면에 필요한 사용자 정보와 등록 물건 요약 리스트를 조회합니다.
     * @param userId 현재 로그인한 사용자 식별자
     * @return 닉네임, 학교, 프로필 사진, 등록 물건 3개가 포함된 DTO
     * @throws CustomException 유저를 찾을 수 없는 경우 발생
     */
    @Transactional(readOnly = true)
    public MyPageMainResDto getMyPageMain(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 해당 유저가 등록한 물건 중 최신순으로 3개만 조회
        List<Item> top3Items = itemRepository.findTop3ByUserOrderByCreatedAtDesc(user);

        return MyPageMainResDto.fromEntity(user, top3Items);
    }

    /**
     * 내가 등록한 아이템 목록을 전체 조회합니다.
     *
     * @param userId 현재 로그인한 사용자의 식별자
     * @return 사용자가 등록한 REGISTERED 상태의 아이템 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyRegisteredItemResDto> getMyRegisteredItems(Long userId) {

        List<Item> items = itemListRepository.findAllByUserIdAndItemStatusOrderByCreatedAtDesc(
                userId, ItemStatus.REGISTERED);

        return items.stream()
                .map(MyRegisteredItemResDto::fromEntity)
                .toList();
    }


    /**
     * 사용자의 프로필(닉네임, 사진)을 수정합니다.
     *
     * @param userId 현재 사용자 ID
     * @param reqDto 수정할 닉네임 정보
     * @param profileImage 새 프로필 이미지 파일 (선택 사항)
     * @return 수정된 프로필 정보 응답 DTO
     */
    @Transactional
    public MyPageProfileResDto updateProfile(Long userId, MyPageProfileReqDto reqDto, MultipartFile profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newNickname = reqDto.getNickname();
        if (newNickname != null && !newNickname.isBlank()) {
            newNickname = newNickname.trim();

            if (!newNickname.equals(user.getNickname())) {
                if (userRepository.existsByNickname(newNickname)) {
                    throw new CustomException(ErrorCode.NICKNAME_CONFLICT);
                }
                user.updateNickname(newNickname);
            }
        }

        // 2. 이미지 업데이트
        if (profileImage != null && !profileImage.isEmpty()) {
            // 기존 이미지가 있다면 삭제 처리 (S3 용량 관리)
            if (user.getProfilePhotoUrl() != null) {
                s3Service.deleteFile(user.getProfilePhotoUrl());
            }

            // 새 이미지 업로드
            String uploadedUrl = s3Service.uploadSingleFile(profileImage);
            if (!uploadedUrl.isEmpty()) {
                user.updateProfileImage(uploadedUrl);
            }
        }

        return MyPageProfileResDto.fromEntity(user);
    }

}
