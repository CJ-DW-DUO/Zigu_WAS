package com.zigu.ziguwas.domains.user.service;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.item.event.ItemImagesDeletedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

        // 요청글(빌려주세요)은 따로 최신순 3개를 담아 내려준다.
        List<Item> top3DemandItems = itemRepository.findTop3ByUserAndPostTypeOrderByCreatedAtDesc(user, PostType.DEMAND);

        return MyPageMainResDto.fromEntity(user, top3Items, top3DemandItems);
    }

    /**
     * 내가 등록한 아이템 목록을 전체 조회합니다.
     *
     * @param userId 현재 로그인한 사용자의 식별자
     * @return 사용자가 등록한 모든 상태(REGISTERED·RENTING)의 아이템 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyRegisteredItemResDto> getMyRegisteredItems(Long userId) {

        List<Item> items = itemListRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return items.stream()
                .map(MyRegisteredItemResDto::fromEntity)
                .toList();
    }


    /**
     * 프로필 수정 화면 진입 시 기존 프로필 정보를 조회합니다.
     *
     * @param userId 현재 사용자 ID
     * @return 이름, 사진, 학번, 성별, 성별 표시 여부
     */
    @Transactional(readOnly = true)
    public MyPageProfileResDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MyPageProfileResDto.fromEntity(user);
    }

    /**
     * 사용자의 프로필(이름, 학번, 성별, 성별 표시 여부, 사진)을 수정합니다.
     * null로 전달된 필드는 변경하지 않습니다.
     *
     * @param userId 현재 사용자 ID
     * @param reqDto 수정할 프로필 정보
     * @param profileImage 새 프로필 이미지 파일 (선택 사항)
     * @return 수정된 프로필 정보 응답 DTO
     */
    @Transactional
    public MyPageProfileResDto updateProfile(Long userId, MyPageProfileReqDto reqDto, MultipartFile profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 프로필 정보 업데이트 (이름은 실명이라 중복 허용)
        String newNickname = reqDto.getNickname();
        if (newNickname != null && !newNickname.isBlank()) {
            user.updateNickname(newNickname.trim());
        }
        if (reqDto.getStudentNumber() != null) {
            user.updateStudentNumber(reqDto.getStudentNumber());
        }
        if (reqDto.getGender() != null) {
            user.updateGender(reqDto.getGender());
        }
        if (reqDto.getGenderVisible() != null) {
            user.updateGenderVisible(reqDto.getGenderVisible());
        }

        // 2. 이미지 업데이트 (새 업로드 성공 → DB 갱신 → 이전 이미지 삭제 순서)
        if (profileImage != null && !profileImage.isEmpty()) {
            String oldImageUrl = user.getProfilePhotoUrl();

            // 새 이미지 업로드 (실패하면 예외로 끝나 기존 이미지는 그대로 남는다)
            String uploadedUrl = s3Service.uploadSingleFile(profileImage);
            if (!uploadedUrl.isEmpty()) {
                user.updateProfileImage(uploadedUrl);

                // 이전 이미지는 DB 커밋이 끝난 뒤 이벤트 리스너가 삭제한다 (실패해도 프로필 수정은 유지, 리스너가 로그를 남김)
                if (oldImageUrl != null) {
                    eventPublisher.publishEvent(new ItemImagesDeletedEvent(List.of(oldImageUrl)));
                }
            }
        }

        return MyPageProfileResDto.fromEntity(user);
    }

}
