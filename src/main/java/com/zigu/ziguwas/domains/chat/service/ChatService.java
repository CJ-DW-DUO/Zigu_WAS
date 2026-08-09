package com.zigu.ziguwas.domains.chat.service;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(room.getId(), user.getId())) {
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
    public List<ChatRoomPreviewResDto> getChatroomsPreview(CustomUserDetails customUserDetails) {

        // 1. 사용자 조회
        User user = userRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 사용자가 속한 채팅방 조회
        List<ChatParticipant> participants = chatParticipantRepository.findAllByUserId(user.getId());

        // 3. 사용자가 속한 방 목록 조회
        List<ChatRoomPreviewResDto> dtos = new ArrayList<>();

        for (ChatParticipant chatParticipant : participants) {

            // 3-1. 채팅방 조회
//            ChatRoom room = chatParticipant.getChatRoom();
            ChatRoom room = chatRoomRepository.findById(
                    chatParticipant.getChatRoomId()
            ).orElseThrow(
                    () -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND)
            );

            // 3-2. 상대 참여자 이름(방이름) 조회

            // 방 기준 모든 참여자 조회 (자기 자신 포함)
            List<ChatParticipant> participant = chatParticipantRepository.findAllByChatRoomId(room.getId());

            // 방 이름 초기화
            String roomName = null;

            // 자기 이름이 아닌 상대방 이름으로 방 이름 설정 (1:1 채팅이므로 상대방은 한명)
            // 만약 채팅방이 여러명으로 이뤄진다면 해당 부분은 수정 필요
            for(ChatParticipant p : participant) {
                if(!p.getUserId().equals(user.getId())) {
                    // 문제가 지금 ChatParticipant에서는 탈퇴한 User가 존재하는데 그거 기반으로 userRepository에서 검색이 안되는 상황임.

                    roomName = userRepository.findById(p.getUserId())
                            .map(User::getNickname)
                            .orElse("탈퇴한 사용자");
                }
            }

            // 만약 방에 상대방이 없다면(자기 자신만 있다면) 예외 처리
            if(roomName == null){
                throw new CustomException(ErrorCode.CHATMATE_NOT_FOUND);
            }

            // 3-3. 마지막으로 보낸 메시지 조회
            ChatMessage lastMessage = chatMessageRepository.findFirstByChatRoomIdOrderByTimestampDesc(chatParticipant.getChatRoomId()).orElse(null);

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
                            .roomId(room.getId())
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
     * @param page 페이지
     * @param size 페이지 사이즈
     * @return 채팅정보
     */
    public List<ChatMessageDetailResDto> getChatroomDetail(CustomUserDetails customUserDetails, String chatRoomId, Integer page, Integer size) {

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
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("timestamp").descending());

        // 5. 채팅방의 메시지 가져오기
        Slice<ChatMessage> messages = chatMessageRepository.findByChatRoomId(room.getId(), pageable);
        List<ChatMessageDetailResDto> dtos = new ArrayList<>();

        // 6. 메시지 dto에 붙이기
        for(ChatMessage cm : messages){
            dtos.add(ChatMessageDetailResDto.builder()
                            .chatRoomId(cm.getChatRoomId())
                            .senderId(cm.getSenderId())
                            .messageId(cm.getId())
                            // 보낸사람을 검색해서 그 사람의 닉네임을 붙이기
                            .senderName(userRepository.findById(cm.getSenderId())
                                    .map(User::getNickname)
                                    .orElse("탈퇴한 사용자"))
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
    public void saveMessage(String message, String email, String chatRoomId) {
        // 1. 발신자 정보 조회
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 해당 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        // 3. 참여자 권한 검증 (선택 사항: 보낸 사람이 해당 방의 참여자인지 확인)
        validateParticipant(chatRoom, sender);

        // 4. 메시지 엔티티 생성 및 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoomId(chatRoom.getId())
                .senderId(sender.getId())
                .message(message)
                .timestamp(LocalDateTime.now())
                .imageUrl(null) // 이미지 URL 필드는 필요 시 추가 처리
                .build();

        chatMessageRepository.save(chatMessage);

        // 5. 같은 채팅방의 참여자 중 발신자를 제외한 사용자에게 알림 이벤트 발행
        List<ChatParticipant> participants = chatParticipantRepository.findAllByChatRoomId(chatRoom.getId());
        for (ChatParticipant participant : participants) {
            if (participant.getUserId().equals(sender.getId())) {
                continue;
            }

            // 5-1. 실제 알림 저장은 NotificationEventListener가 AFTER_COMMIT 시점에 처리
            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    participant.getUserId(),
                    NotificationType.CHAT,
                    "새 채팅 메시지",
                    sender.getNickname() + "님: " + message,
                    chatRoom.getId()
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
    public String createChatRoom(CustomUserDetails details, CreateChatRoomReqDto dto) {

        // 1. 물품 조회
        Item item = itemRepository.findById(dto.getItemId()).orElseThrow(
                () -> new CustomException(ErrorCode.ITEM_NOT_FOUND)
        );

        // 채팅을 만드려고 하는 사람(임차인)이 해당 물품을 업로드한 사람(임대인)의 대학과 다르면 접근 제한
        if(!item.getUser().getUniv().getUnivId().equals(details.getUnivId())) {
            throw new CustomException(ErrorCode.DIFFERENT_UNIVERSITY_ACCESS);
        }

        // 2. 사용자 조회
        User sender = userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // ID 누락여부 확인
        if(dto.getReceiverId() == null) {
            throw new CustomException(ErrorCode.MISS_USER_ID);
        }

        // 대화 대상자 조회
        User receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 해당 물품과 거래로 이루어진 채팅방이 이미 존재한다면, 그냥 해당 ID를 반환
//        ChatRoom chatRoom = chatRoomRepository.findByItemAndParticipants(item, sender, receiver).orElse(null);

        // 4. sender 참여 방 ID 목록
        Set<String> senderRoomIds = chatParticipantRepository.findAllByUserId(sender.getId())
                // 리스트를 스트림 파이프라인으로 변환
                .stream()
                // 각 ChatParticipant 객체에서 chatRoomId(String)만 꺼내기 (p -> p.getChatRoomId()로 쓸 수 있음.)
                .map(ChatParticipant::getChatRoomId)
                // 결과 모으기 (중복 제거를 위해 Set으로)
                .collect(Collectors.toSet());

        // 5. receiver 참여 목록 중 sender와 겹치는 방 찾기
        Optional<ChatRoom> existingRoom = chatParticipantRepository.findAllByUserId(receiver.getId())
                .stream()
                // receiver의 참여 목록 중 chatRoomId가 sender의 방 ID 목록에 포함되는 것만 남김. 즉 두 사람이 같이 있는 방만 걸러냄(남은요소)
                .filter(p -> senderRoomIds.contains(p.getChatRoomId()))
                // 걸러진 결과로 chatRoom객체를 MongoDB에서 조회하여 반환받음(새배열)
                .map(p -> chatRoomRepository.findById(p.getChatRoomId()).orElse(null))
                // 조회된 ChatRoom 중 itemId가 요청한 item과 일치하는 것만 남김. 즉, 두 사람이 참여하고 있는 방 중에서 해당 물품과 연결된 방만 남김(남은 요소)
                .filter(room -> room != null && room.getItemId().equals(item.getId()))
                // 조건을 만족하는 첫번째 ChatRoom을 Optional로 반환.
                .findFirst();

        if (existingRoom.isPresent()) {
            return existingRoom.get().getId();
        }

        // 6. 채팅방 생성 (존재하는 채팅방이 없을 경우)
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder().itemId(item.getId()).build());

        // 7. 참여자 등록 (발신자, 수신자 모두 등록)
        chatParticipantRepository.save(ChatParticipant.builder().chatRoomId(chatRoom.getId()).userId(sender.getId()).isParticipating(true).build());
        chatParticipantRepository.save(ChatParticipant.builder().chatRoomId(chatRoom.getId()).userId(receiver.getId()).isParticipating(true).build());

        // 8. 채팅방 ID 반환
        return chatRoom.getId();
    }

    /**
     * 채팅방과 연결된 아이템 정보 및 거래 상태 조회
     *
     * @param customUserDetails 로그인 정보
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 아이템 및 거래 정보 DTO
     */
    public ChatRoomItemAndTradeInfoResDto getChatroomItemAndTradeInfo(CustomUserDetails customUserDetails, String chatRoomId) {

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

        // 4. 채팅방과 연결된 아이템 조회 (소프트 딜리트된 아이템이면 DELETED_ITEM 반환)
        Item item = itemRepository.findById(chatRoom.getItemId()).orElseThrow(
                () -> new CustomException(ErrorCode.DELETED_ITEM)
        );

        // 5. 거래상태 조회 (아이템 + 사용자 기준)
        // Trade는 chatRoom과 직접 연결되지 않으므로, 채팅방의 아이템과 현재 사용자로 거래를 조회한다.
        // 현재 사용자가 임대인(renter)인 거래를 먼저 찾고, 없으면 임차인(rentee)인 거래를 찾는다.
        Trade trade = tradeRepository.findByItemAndRenter(item, user)
                .or(() -> tradeRepository.findByItemAndRentee(item, user))
                .orElse(null);

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
