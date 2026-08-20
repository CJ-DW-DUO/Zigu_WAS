package com.zigu.ziguwas.domains.item.service;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.item.dto.request.ItemRegisterReqDto;
import com.zigu.ziguwas.domains.item.dto.request.ItemUpdateReqDto;
import com.zigu.ziguwas.domains.item.dto.response.ItemResDto;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.item.event.ItemImagesDeletedEvent;
import com.zigu.ziguwas.domains.item.repository.ItemImageRepository;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemImageRepository itemImageRepository;
    private final TradeRepository tradeRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 새로운 아이템을 등록합니다.
     *
     * @param itemRegisterReqDto 등록할 아이템 정보가 담긴 DTO
     * @param userId 아이템을 등록하는 사용자의 ID
     * @return 등록된 아이템 정보를 담은 ItemResDto
     * @throws CustomException 사용자를 찾을 수 없을 때 (ErrorCode.USER_NOT_FOUND) 발생
     */
    @Transactional
    public ItemResDto registerItem(ItemRegisterReqDto itemRegisterReqDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRegisterReqDto.toEntity(user);

        Item savedItem = itemRepository.save(item);

        return ItemResDto.fromEntity(savedItem, userId);
    }

    /**
     * 특정 아이템에 여러 장의 이미지를 업로드하고 연결합니다.
     *
     * @param itemId 이미지를 추가할 아이템의 ID
     * @param images 업로드할 이미지 파일 리스트
     * @param userId 요청을 보낸 사용자의 ID (권한 확인용)
     * @return 이미지가 추가된 아이템 정보를 담은 ItemResDto
     * @throws CustomException
     * - ErrorCode.NOT_FOUND_ITEM: 해당 ID의 아이템이 존재하지 않을 경우
     * - ErrorCode.UNAUTHORIZED_ACCESS: 아이템 소유자가 아닐 경우
     */
    @Transactional
    public ItemResDto uploadImages(Long itemId, List<MultipartFile> images, Long userId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        List<String> uploadedUrls = s3Service.uploadFiles(images);

        // 기존에 이미지가 하나도 없었는지 확인
        boolean isFirstImage = item.getImageUrl().isEmpty();

        for (int i = 0; i < uploadedUrls.size(); i++) {
            // 첫 번째 업로드되는 파일이면서, 기존 이미지가 없을 때만 true
            boolean isMain = isFirstImage && (i == 0);
            item.addImage(new ItemImage(uploadedUrls.get(i), isMain));
        }

        return ItemResDto.fromEntity(item, userId);
    }

    /**
     * 특정 아이템의 이미지를 삭제합니다.
     * S3 저장소에 저장된 실제 이미지 파일을 삭제한 후,
     * 데이터베이스(DB)에서 해당 이미지를 제거합니다.
     *
     * @param itemId  이미지가 속한 아이템의 ID (검증용)
     * @param imageIds 삭제할 이미지들의 고유 ID 리스트
     * @param userId 아이템을 등록하는 사용자의 ID
     * @throws CustomException
     * - ErrorCode.NOT_FOUND_IMAGE: 해당 ID의 이미지가 존재하지 않을 경우
     * - ErrorCode.IMAGE_NOT_BELONG_TO_ITEM: 해당 image의 주인이 해당 item이 아닌경우
     * - ErrorCode.UNAUTHORIZED_ACCESS: 이미지 ID 검증에 실패했을 경우
     */
    @Transactional
    public void deleteImage(Long itemId, List<Long> imageIds, Long userId) {

        List<ItemImage> itemImages = itemImageRepository.findAllById(imageIds);

        if(itemImages.isEmpty()) {return;}

        for (ItemImage img : itemImages) {
            if (!img.getItem().getId().equals(itemId)) {
                throw new CustomException(ErrorCode.IMAGE_NOT_BELONG_TO_ITEM);
            }
            if (!img.getItem().getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
            }
        }

        Item item = itemImages.get(0).getItem();
        boolean isMainDeleted = itemImages.stream().anyMatch(ItemImage::isMainImageUrl);
        List<String> imageUrlsToDelete = itemImages.stream().map(ItemImage::getImageUrl).toList();

        // JPQL 벌크 삭제 → @SQLDelete 우회하여 하드딜리트
        itemImageRepository.deleteAllInBatch(itemImages);

        // 메인 이미지가 삭제 목록에 있었다면 DB에서 직접 조회하여 새로운 메인 지정
        if (isMainDeleted) {
            itemImageRepository.findFirstByItemOrderByImageIdAsc(item)
                    .ifPresent(nextMain -> nextMain.updateMain(true));
        }

        // S3 파일 삭제는 DB 커밋 이후로 분리 (S3 호출이 느려져도 DB 트랜잭션/락을 붙잡지 않도록)
        eventPublisher.publishEvent(new ItemImagesDeletedEvent(imageUrlsToDelete));
    }

    /**
     * 기존 아이템의 정보를 수정합니다.
     * @param itemId             수정할 아이템의 ID
     * @param itemUpdateReqDto   수정할 정보가 담긴 DTO
     * @param userId             수정을 요청한 사용자의 ID (권한 확인용)
     * @return                   수정된 아이템 정보 (ItemResDto)
     * @throws CustomException
     * - UNAUTHORIZED_ACCESS: 요청 유저와 아이템 작성자가 일치하지 않을 경우
     */
    @Transactional
    public ItemResDto updateItem(Long itemId, ItemUpdateReqDto itemUpdateReqDto, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        item.updateItemPost(
                itemUpdateReqDto.getTitle(),
                itemUpdateReqDto.getCategory(),
                itemUpdateReqDto.getDayPerPrice(),
                itemUpdateReqDto.getDescription()
        );

        return ItemResDto.fromEntity(item, userId);
    }

    /**
     * 아이템 게시글을 삭제합니다.
     *
     * @param itemId 삭제할 아이템의 고유 식별자
     * @param userId 삭제를 요청한 유저의 고유 식별자 (권한 확인용)
     * @throws CustomException
     * - NOT_FOUND_ITEM: 삭제하려는 아이템이 존재하지 않을 경우
     * - UNAUTHORIZED_ACCESS: 요청 유저와 아이템 작성자가 일치하지 않을 경우
     */
    @Transactional
    public void deleteItem(Long itemId, Long userId){
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 대기 중(REQUESTED) 또는 대여 중(IN_PROGRESS)인 거래가 있으면 삭제 불가
        boolean hasActiveTrade = tradeRepository.existsByItemAndTradeStatusIn(
                item, List.of(TradeStatus.REQUESTED, TradeStatus.IN_PROGRESS)
        );
        if (hasActiveTrade) {
            throw new CustomException(ErrorCode.ACTIVE_TRADE_EXISTS);
        }

        List<String> imageUrlsToDelete = item.getImageUrl().stream().map(ItemImage::getImageUrl).toList();

        itemRepository.delete(item);

        // S3 파일 삭제는 DB 커밋 이후로 분리 (S3 호출이 느려져도 DB 트랜잭션/락을 붙잡지 않도록)
        eventPublisher.publishEvent(new ItemImagesDeletedEvent(imageUrlsToDelete));
    }

    /**
     *
     * @param itemId 해당 item을 조회합니다.
     * @param userId 조회 요청을 보낸 유저 (이미 인증 됨)
     * @return 해당 item 정보
     */
    @Transactional
    public ItemResDto getItemDetail(Long itemId, Long userId, Long univId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        try {
            // 주인이 없거나(null), 탈퇴한 유저라면 상세조회 차단
            if (item.getUser() == null || item.getUser().isDeleted()) {
                throw new CustomException(ErrorCode.WITHDRAWN_USER_ITEM);
            }

            // 허용된 대학의 인원이 아니라면(univId가 다르다면)
            if (!item.getUser().getUniv().getUnivId().equals(univId)) {
                throw new CustomException(ErrorCode.DIFFERENT_UNIVERSITY_ACCESS);
            }

        } catch (EntityNotFoundException e) {
            // 하이버네이트 프록시가 유저를 찾지 못할 때 (이미 탈퇴한 경우) 발생하는 에러 처리
            throw new CustomException(ErrorCode.WITHDRAWN_USER_ITEM);
        }

        // 조회수 증가는 상세조회 결과에 영향을 주지 않는 부가 기능이므로,
        // 실패하더라도 상세조회 자체는 정상 응답하도록 best-effort로 처리
        try {
            itemRepository.increaseViewCount(itemId);
        } catch (Exception e) {
            log.warn("조회수 증가 실패 - itemId: {}", itemId, e);
        }

        return ItemResDto.fromEntity(item, userId);
    }

}