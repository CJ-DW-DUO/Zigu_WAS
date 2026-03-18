package com.zigu.ziguwas.domains.user.service;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyPageMainResDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    /**
     * 마이페이지 메인 화면에 필요한 사용자 정보와 등록 물건 요약 리스트를 조회합니다.
     * @param userId 현재 로그인한 사용자 식별자
     * @return 닉네임, 학교, 프로필 사진, 등록 물건 3개가 포함된 DTO
     * @throws CustomException 유저를 찾을 수 없는 경우 발생
     */
    public MyPageMainResDto getMyPageMain(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 해당 유저가 등록한 물건 중 최신순으로 3개만 조회
        List<Item> top3Items = itemRepository.findTop3ByUserOrderByCreatedAtDesc(user);

        return MyPageMainResDto.fromEntity(user, top3Items);
    }
}
