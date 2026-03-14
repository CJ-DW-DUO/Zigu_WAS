package com.zigu.ziguwas.domains.item.service;

import com.zigu.ziguwas.S3.S3Service;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    /**
     * 새로운 아이템을 등록합니다.
     *
     * @param itemRegisterReqDto 등록할 아이템 정보가 담긴 DTO
     * @param userId 아이템을 등록하는 사용자의 ID
     * @return 등록된 아이템 정보를 담은 ItemResDto
     * @throws CustomException 사용자를 찾을 수 없을 때 (ErrorCode.NOT_FOUND_USER) 발생
     */
    @Transactional
    public ItemResDto registerItem(ItemRegisterReqDto itemRegisterReqDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
        Item item = itemRegisterReqDto.toEntity(user);

        Item savedItem = itemRepository.save(item);

        return ItemResDto.fromEntity(savedItem);
    }

    /**
     * 특정 아이템에 여러 장의 이미지를 업로드하고 연결합니다.
     *
     * @param itemId 이미지를 추가할 아이템의 ID
     * @param images 업로드할 이미지 파일 리스트
     * @return 이미지가 추가된 아이템 정보를 담은 ItemResDto
     * @throws CustomException 해당 ID의 아이템이 존재하지 않을 경우 (ErrorCode.NOT_FOUND_ITEM) 발생
     */
    @Transactional
    public ItemResDto uploadImages(Long itemId, List<MultipartFile> images) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ITEM));

        List<String> uploadedUrls = s3Service.uploadFiles(images);

        for (String url : uploadedUrls) {
            item.addImage(new ItemImage(url));
        }
        return ItemResDto.fromEntity(item);
    }
}