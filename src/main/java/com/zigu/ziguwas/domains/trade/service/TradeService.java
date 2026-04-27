package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemStatus;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.notification.entity.NotificationType;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.trade.dto.request.TradeOfferReqDto;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    // 거래 상태 변경 시 알림 이벤트를 발행하기 위한 퍼블리셔
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 사용자 불러오기
     *
     * @param email 사용자 이메일
     * @return 사용자 엔티티
     */
    private User getCurrentUser(String email){
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );
    }

    /**
     * 거래 불러오기
     *
     * @param tradeId 거래ID
     * @return 거래 엔티티
     */
    private Trade getCurrentTrade(Long tradeId){
        return tradeRepository.findById(tradeId).orElseThrow(
                () -> new CustomException(ErrorCode.TRADE_NOT_FOUND)
        );
    }


    /**
     * 대여 요청 서비스
     *
     * @param details 임차예정인 로그인 정보
     * @param dto 아이템ID, 매물 대여기간
     */
    @Transactional
    public Long tradeOffer(
            CustomUserDetails details,
            TradeOfferReqDto dto
    ){
        // 1. 임차인 조회(로그인 정보 검증)
        User rentee = getCurrentUser(details.getUsername());

        // 2. 매물 조회
        Item item = itemRepository.findById(dto.getItemId()).orElseThrow(
                () -> new CustomException(ErrorCode.ITEM_NOT_FOUND)
        );

        // 3. 임대인 조회
        User renter = item.getUser();


        // 4. 임대인이랑 임차인이 같으면 안됨
        if(rentee.getId().equals(renter.getId())){
            throw new CustomException(ErrorCode.SELF_TRADE_NOT_ALLOWED);
        }

        // 5. 요청 전송, 거래 시작, 수락일은 요청 당시에 존재하지 않으므로 미기입
        Trade trade = Trade.builder()
                .item(item) // 매물 연결
                .renter(renter) // 임대인 연결
                .rentee(rentee) // 임차인 연결
                .tradeStdate(dto.getStartDate()) // 거래 시작 예정일 우선 기입
                .tradeEndate(dto.getEndDate()) // 거래 종료 예정일 우선 기입
                .period(ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate())) // 대여 기간
                .tradeStatus(TradeStatus.REQUESTED) // 요청됨
                .tradeReqdate(LocalDate.now()) // 요청은 지금 보내는 것
                .build();

        // 6. 거래 요청 저장
        Trade saved = tradeRepository.save(trade);

        // 7. 임대인에게 대여 요청 알림 이벤트 발행
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                renter.getId(),
                NotificationType.RENTAL_REQUEST,
                "새로운 대여 요청",
                rentee.getNickname() + "님이 " + item.getTitle() + " 대여를 요청했어요."
        ));

        // 6. 거래ID 반환
        return saved.getId();
    }
    

    /**
     * 대여 제안 응답 서비스 -> 승인 혹은 거절
     *
     * @param details 임대인 로그인 정보
     * @param tradeId 거래ID
     * @param isApproved 대여 승인/거절 여부
     */
    @Transactional
    public void offerResponse(
            CustomUserDetails details,
            Long tradeId,
            boolean isApproved
    ){
        // 1. 임대인 조회
        User renter = getCurrentUser(details.getUsername());

        // 2. 거래 조회
        Trade trade = getCurrentTrade(tradeId);

        // 3. 매물 조회
        Item item = trade.getItem();
        if(item == null){
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        // 4. 임대인 일치 확인
        if(!renter.getId().equals(trade.getRenter().getId())) {
            throw new CustomException(ErrorCode.RENTER_NOT_MATCHED);
        }

        // 5. 거래 요청 상태 확인(대여 오류 방지)
        if(!trade.getTradeStatus().equals(TradeStatus.REQUESTED)) {
            throw new CustomException(ErrorCode.TRADE_STATUS_NOT_REQUESTED);
        }

        // 6. 매물 대여 가능 확인
        if(item.getItemStatus().equals(ItemStatus.RENTING)){
            throw new CustomException(ErrorCode.ITEM_ALREADY_RENTING);
        }

        // 7. 대여 승인 혹은 거절 분기
        if(isApproved){
            // 대여 승인

            // 7A - 1. 거래 상태 변경
            trade.updateStatus(TradeStatus.IN_PROGRESS);

            // 7A - 2. 거래 시작일, 마감일, 수락일 설정
            // 거래 시작일과 수락일에 대한 분리가 필요함, 수정 필수
            trade.setDates(LocalDate.now(), LocalDate.now(),
                    LocalDate.now().plusDays(trade.getPeriod()));

            // 7A - 3. 매물 상태 변경
            item.updateItemStatus(ItemStatus.RENTING);

            // 7A - 4. 매물 변경상태 저장
            itemRepository.save(item);
        } else {
            // 대여 거절

            // 7B. 거래 상태 변경
            trade.updateStatus(TradeStatus.REJECTED);
        }

        // 8. 거래 변경 상태 저장
        tradeRepository.save(trade);

        // 9. 승인/거절 여부에 따라 임차인에게 보낼 알림 메시지 구성
        NotificationType notificationType = isApproved
                ? NotificationType.RENTAL_ACCEPT
                : NotificationType.RENTAL_REJECT;
        String title = isApproved ? "대여 요청 승인" : "대여 요청 거절";
        String content = isApproved
                ? "요청한 " + item.getTitle() + " 대여가 승인되었습니다."
                : "요청한 " + item.getTitle() + " 대여가 거절되었습니다.";

        // 10. 임차인에게 승인/거절 알림 이벤트 발행
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                trade.getRentee().getId(),
                notificationType,
                title,
                content
        ));
    }

    /**
     * 반납 확인 서비스
     *
     * 임대인과 임차인이 한 대여에 대해서 상호로 반납여부를 확인한다.
     *
     * @param details 임대인 / 임차인 로그인 여부
     * @param tradeId 거래ID
     */
    public void returnTradeCheck(
            CustomUserDetails details,
            Long tradeId
    ){
        // 1. 사용자 조회
        User user = getCurrentUser(details.getUsername());

        // 2. 거래조회
        Trade trade = getCurrentTrade(tradeId);

        // 3. 매물 조회
        Item item = trade.getItem();
        if(item == null){
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        // 4. 임대인 일치 확인
        if(!user.getId().equals(trade.getRenter().getId())){
            throw new CustomException(ErrorCode.RENTER_NOT_MATCHED);
        }

        // 5. 아이템 상태 확인
        if(!item.getItemStatus().equals(ItemStatus.RENTING)){
            throw new CustomException(ErrorCode.ITEM_NOT_RENTING);
        }

        // 6. 거래 상태 확인
        if(!trade.getTradeStatus().equals(TradeStatus.IN_PROGRESS)){
            throw new CustomException(ErrorCode.TRADE_STATUS_NOT_REQUESTED);
        }

        // 7. 상태변경
        item.updateItemStatus(ItemStatus.REGISTERED);
        trade.updateStatus(TradeStatus.RETURNED);

        // 8. 변경 반영
        itemRepository.save(item);
        tradeRepository.save(trade);

        // 9. 임차인에게 반납 완료 알림 이벤트 발행
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                trade.getRentee().getId(),
                NotificationType.RETURN_COMPLETE,
                "반납 처리 완료",
                item.getTitle() + "의 반납 처리가 완료되었습니다."
        ));
    }
}
