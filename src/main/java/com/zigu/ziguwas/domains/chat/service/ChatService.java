package com.zigu.ziguwas.domains.chat.service;

import com.zigu.ziguwas.domains.chat.dto.request.ChatMessagePageReqDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatMessageDetailResDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomPreviewResDto;
import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatMessageRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.notification.entity.NotificationType;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
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
            String roomName = userRepository.findById(chatParticipant.getUser().getId()).orElseThrow(
                    () -> new CustomException(ErrorCode.CHATMATE_NOT_FOUND)
            ).getNickname();

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

        // 3. 페이지 범위 설정하기
        Pageable pageable = PageRequest.of(dto.getPage(), dto.getSize(),
                Sort.by("timestamp").descending());

        // 4. 채팅방의 메시지 가져오기
        Slice<ChatMessage> messages = chatMessageRepository.findByChatRoom(room, pageable);
        List<ChatMessageDetailResDto> dtos = new ArrayList<>();

        // 5. 메시지 dto에 붙이기
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
     * @param receiverId 채팅 참여자
     * @return 채팅방 ID
     */
    @Transactional
    public Long createChatRoom(CustomUserDetails details, Long receiverId) {
        // 1. 사용자 조회
        User sender = userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(receiverId == null) {
            throw new CustomException(ErrorCode.MISS_USER_ID);
        }

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 기존 1:1 채팅방이 있으면 재사용
        ChatRoom existingRoom = chatRoomRepository.findOneByParticipants(sender, receiver).orElse(null);
        if (existingRoom != null) {

            // 전송자 조회
            ChatParticipant senderParticipant = chatParticipantRepository.findByChatRoomAndUser(existingRoom, sender)
                    .orElseThrow( () -> new CustomException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND));

            // 타 품목으로 채팅방이 존재할 수 있기 때문에 참여상태 true로 변경
            senderParticipant.setParticipating(true);
            chatParticipantRepository.save(senderParticipant);

            // 대상자 조회
            ChatParticipant receiverParticipant = chatParticipantRepository.findByChatRoomAndUser(existingRoom, receiver)
                    .orElseThrow( () -> new CustomException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND));

            // 타 품목으로 채팅방이 존재할 수 있기 때문에 참여상태 true로 변경
            receiverParticipant.setParticipating(true);
            chatParticipantRepository.save(receiverParticipant);

            // 이미 개설된 채팅방 ID 재활용
            return existingRoom.getChatId();
        }

        // 3. 채팅방 신규 생성
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom());

        // 4. 참여자 등록 (발신자, 수신자 모두 등록)
        chatParticipantRepository.save(ChatParticipant.builder()
                .chatRoom(chatRoom)
                .user(sender)
                .isParticipating(true)
                .build());
        chatParticipantRepository.save(ChatParticipant.builder()
                .chatRoom(chatRoom)
                .user(receiver)
                .isParticipating(true)
                .build());

        // 5. 채팅방 ID 반환
        return chatRoom.getChatId();
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
