package com.zigu.ziguwas.domains.chat.service;

import com.zigu.ziguwas.domains.chat.dto.request.ChatMessagePageReqDto;
import com.zigu.ziguwas.domains.chat.dto.request.CreateChatRoomReqDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatMessageDetailResDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomItemAndTradeInfoResDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomPreviewResDto;
import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatMessageRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.notification.entity.NotificationType;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;
    // 채팅/거래 등 도메인 서비스에서 알림 이벤트를 발행할 때 사용
    private final ApplicationEventPublisher eventPublisher;
    private final TradeRepository tradeRepository;
    private final ItemRepository itemRepository;

    /**
     * 채팅방에 해당 유저가 존재하는지 확인하는 서비스
     *
     * @param room 채팅방
     * @param user 사용자
     */
    private void validateParticipant(ChatRoom room, User user) {
        if (!chatParticipantRepository.existsByChatRoomAndUser(room, user)) {
            // 채팅방에 해당 유저가 없으므로 접근 제한
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    /**
     * 참여중인 채팅방 조회 DTO
     *
     * @param customUserDetails 로그인 사용자 정보
     * @return 사용자가 참여중인 채팅방 미리보기 목록
     */
    @Transactional
    public List<ChatRoomPreviewResDto> getChatroomsPreview(CustomUserDetails customUserDetails) {

        // 1. 사용자 조회
        User user = userRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 사용자가 속한 채팅방 조회
        List<ChatParticipant> participants = chatParticipantRepository.findAllByUser(user);

        // 3. 사용자가 속한 방 목록 조회
        List<ChatRoomPreviewResDto> dtos = new ArrayList<>();

        for (ChatParticipant chatParticipant : participants) {

            // 3-1. 채팅방 조회
            ChatRoom room = chatParticipant.getChatRoom();

            // 3-2. 상대 참여자 이름(방이름) 조회

            // 방 기준 모든 참여자 조회 (자기 자신 포함)
            List<ChatParticipant> participant = chatParticipantRepository.findAllByChatRoom(room);

            // 방 이름 초기화
            String roomName = null;

            // 자기 이름이 아닌 상대방 이름으로 방 이름 설정 (1:1 채팅이므로 상대방은 한명)
            // 만약 채팅방이 여러명으로 이뤄진다면 해당 부분은 수정 필요
            for(ChatParticipant p : participant) {
                if(!p.getUser().equals(user)) {
                    roomName = p.getUser().getNickname();
                }
            }

            // 만약 방에 상대방이 없다면(자기 자신만 있다면) 예외 처리
            if(roomName == null){
                throw new CustomException(ErrorCode.CHATMATE_NOT_FOUND);
            }

            // 3-3. 마지막으로 보낸 메시지 조회
            ChatMessage lastMessage = chatMessageRepository.findFirstByChatRoomOrderByTimestampDesc(chatParticipant.getChatRoom()).orElse(null);

            // 3-4. 마지막으로 대화한 시각 조회

            LocalDateTime lastSendTime = null;
            String lastMessageContent;

            if (lastMessage == null) {
                lastMessageContent = "대화 내역이 없습니다.";
            } else {
                lastSendTime = lastMessage.getTimestamp();
                lastMessageContent = lastMessage.getMessage();
            }

            // 3-5. dto에 담기
            dtos.add(ChatRoomPreviewResDto.builder()
                            .roomId(room.getChatId())
                            .roomName(roomName)
                            .lastMessage(lastMessageContent)
                            .lastMessageTimestamp(lastSendTime)
                            .build());
        }

        return dtos;
    }

    /**
     * 채팅방 상세조회
     *
     * @param customUserDetails 로그인정보
     * @param chatRoomId 채팅방ID
     * @param dto 페이지 요청 정보
     * @return 채팅정보
     */
    @Transactional
    public List<ChatMessageDetailResDto> getChatroomDetail(CustomUserDetails customUserDetails, Long chatRoomId, ChatMessagePageReqDto dto) {

        // 1. 유저 확인
        User user = userRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 채팅방의 존재여부 확인
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElseThrow(
                () -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND)
        );

        // 3. 참가자 유효성 검증
        validateParticipant(room, user);

        // 4. 페이지 범위 설정하기
        Pageable pageable = PageRequest.of(dto.getPage(), dto.getSize(),
                Sort.by("timestamp").descending());

        // 5. 채팅방의 메시지 가져오기
        Slice<ChatMessage> messages = chatMessageRepository.findByChatRoom(room, pageable);
        List<ChatMessageDetailResDto> dtos = new ArrayList<>();

        // 6. 메시지 dto에 붙이기
        for(ChatMessage cm : messages){
            dtos.add(ChatMessageDetailResDto.builder()
                            .chatRoomId(cm.getChatRoom().getChatId())
                            .senderId(cm.getSender().getId())
                            .messageId(cm.getMessageId())
                            .senderName(cm.getSender().getNickname())
                            .message(cm.getMessage())
                            .timestamp(cm.getTimestamp().toString())
                            .imageUrl(cm.getImageUrl())
                    .build());
        }

        return dtos;
    }


    /**
     * 실시간 메시지 저장
     *
     * @param message 메시지
     * @param email 이메일
     * @param chatRoomId 채팅방ID
     */
    @Transactional
    public void saveMessage(String message, String email, Long chatRoomId) {
        // 1. 발신자 정보 조회
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 해당 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        // 3. 참여자 권한 검증 (선택 사항: 보낸 사람이 해당 방의 참여자인지 확인)
        validateParticipant(chatRoom, sender);

        // 4. 메시지 엔티티 생성 및 저장
        ChatMessage chatMessage = new ChatMessage(
                null,
                chatRoom,
                sender,
                message,
                LocalDateTime.now(),
                null // 이미지 URL 필드는 필요 시 추가 처리
        );

        chatMessageRepository.save(chatMessage);

        // 5. 같은 채팅방의 참여자 중 발신자를 제외한 사용자에게 알림 이벤트 발행
        List<ChatParticipant> participants = chatParticipantRepository.findAllByChatRoom(chatRoom);
        for (ChatParticipant participant : participants) {
            User receiver = participant.getUser();
            if (receiver.getId().equals(sender.getId())) {
                continue;
            }

            // 5-1. 실제 알림 저장은 NotificationEventListener가 AFTER_COMMIT 시점에 처리
            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    receiver.getId(),
                    NotificationType.CHAT,
                    "새 채팅 메시지",
                    sender.getNickname() + "님: " + message
            ));
        }
    }

    /**
     * 1대1 채팅방 생성
     *
     * @param details 채팅 생성자 로그인정보
     * @param dto 채팅 대상자 및 물품 정보
     * @return 채팅방 ID
     */
    @Transactional
    public Long createChatRoom(CustomUserDetails details, CreateChatRoomReqDto dto) {

        // 1. 물품 조회
        Item item = itemRepository.findById(dto.getItemId()).orElseThrow(
                () -> new CustomException(ErrorCode.ITEM_NOT_FOUND)
        );

        // 2. 사용자 조회
        User sender = userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(dto.getReceiverId() == null) {
            throw new CustomException(ErrorCode.MISS_USER_ID);
        }

        User receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 해당 물품과 거래로 이루어진 채팅방이 이미 존재하는지
        if (chatRoomRepository.findByItemAndParticipants(item, sender, receiver).isPresent()) {
            throw new CustomException(ErrorCode.CHATROOM_ALREADY_EXISTS);
        }

        // 4. 채팅방 생성
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder().item(item).build());

        // 5. 참여자 등록 (발신자, 수신자 모두 등록)
        chatParticipantRepository.save(ChatParticipant.builder().chatRoom(chatRoom).user(sender).isParticipating(true).build());
        chatParticipantRepository.save(ChatParticipant.builder().chatRoom(chatRoom).user(receiver).isParticipating(true).build());

        // 6. 채팅방 ID 반환
        return chatRoom.getChatId();
    }

    /**
     * 채팅방과 연결된 아이템 정보 및 거래 상태 조회
     *
     * @param customUserDetails 로그인 정보
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 아이템 및 거래 정보 DTO
     */
    public ChatRoomItemAndTradeInfoResDto getChatroomItemAndTradeInfo(CustomUserDetails customUserDetails, Long chatRoomId) {

        // 1. 사용자 조회
        User user = userRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(
                () -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND)
        );

        // 3. 참가자 유효성 검증
        validateParticipant(chatRoom, user);

        // 4. 채팅방과 연결된 아이템 조회
        Item item = chatRoom.getItem();
        if (item == null) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        // 5. 거래상태 조회 (채팅방 기준)
        Trade trade = tradeRepository.findByChatRoom(chatRoom).orElse(null);

        String imageUrl = null;
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            imageUrl = item.getImageUrl().get(0).toString();
        }

        // 6. 반환
        return ChatRoomItemAndTradeInfoResDto.builder()
                .chatroomId(chatRoomId)
                .itemId(item.getId())
                .itemTitle(item.getTitle())
                .itemPrice(item.getDayPerPrice())
                .imageUrl(imageUrl)
                .userRole((item.getUser().getId().equals(user.getId())) ? "RENTER" : "RENTEE")
                .tradeStatus((trade != null) ? trade.getTradeStatus().toString() : "NO_TRADE")
                .build();
    }

//    @Transactional
//    public void deleteChatRoom(Long chatRoomId) {
//
//        if(!chatRoomRepository.existsById(chatRoomId)) {
//            throw new CustomException(ErrorCode.CHATROOM_NOT_FOUND);
//        }
//
//        chatRoomRepository.deleteById(chatRoomId);
//    }


}
