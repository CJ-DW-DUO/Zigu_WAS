package com.zigu.ziguwas.domains.item.service;

import com.zigu.ziguwas.domains.item.dto.reqdto.ItemRegisterReqDto;
import com.zigu.ziguwas.domains.item.dto.resdto.ItemResDto;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    /**
     * 새로운 물건(Item)을 등록합니다.
     * 사용자가 업로드한 이미지 URL 리스트를 기반으로 ItemImage를 생성하고,
     * 양방향 연관관계를 설정한 뒤 영속성 전이(Cascade)를 통해 일괄 저장합니다.
     *
     * @param itemRegisterReqDto 물건 등록에 필요한 데이터가 담긴 DTO
     * @return 저장된 물건의 식별자(ID)
     */
    @Transactional
    public ItemResDto registerItem(ItemRegisterReqDto itemRegisterReqDto , String email) {
        User user = userRepository.findByEmail(email).orElseThrow(()-> new CustomException(ErrorCode.NOT_FOUND_USER));
        Item item = itemRegisterReqDto.toEntity(user);

        // DTO에 담긴 사진 URL 리스트를 순회하며 ItemImage 객체 생성
        itemRegisterReqDto.getImageUrl().forEach(url -> {
            item.addImage(ItemImage.builder().imageUrl(url).build());
        });

        // CascadeType.ALL 설정으로 인해 ItemImage들도 자동 저장
        return ItemResDto.fromEntity(itemRepository.save(item));
    }
}
