package com.zigu.ziguwas.domains.chat.service;

import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomPreviewResDto;
import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatMessageRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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
     * @param customUserDetails
     * @return
     */
    public List<ChatRoomPreviewResDto> getMyChatroomsPreview(CustomUserDetails customUserDetails) {

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
            ChatMessage lastMessage = chatMessageRepository.findFirstByChatRoomOrderByTimestampDesc(chatParticipant.getChatRoom()).orElseThrow(
                    () -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND)
            );

            // 3-4. 마지막으로 대화한 시각 조회
            LocalDateTime lastSendTime = lastMessage.getTimestamp();

            // 3-5. dto에 담기
            dtos.add(ChatRoomPreviewResDto.builder()
                            .roomId(room.getChatId())
                            .roomName(roomName)
                            .lastMessage(lastMessage.getMessage())
                            .lastMessageTimestamp(lastSendTime)
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
                "" // 이미지 URL 필드는 필요 시 추가 처리
        );

        chatMessageRepository.save(chatMessage);
    }

    /**
     * 1대1 채팅방 생성
     *
     * @param senderId 채팅 생성자
     * @param receiverId 채팅 참여자
     * @return 채팅방 ID
     */
    @Transactional
    public Long createChatRoom(Long senderId, Long receiverId) {
        // 1. 채팅방 생성
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom());

        // 2. 사용자 조회
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 참여자 등록 (발신자, 수신자 모두 등록)
        chatParticipantRepository.save(ChatParticipant.builder().chatRoom(chatRoom).user(sender).build());
        chatParticipantRepository.save(ChatParticipant.builder().chatRoom(chatRoom).user(receiver).build());

        // 4. 채팅방 ID 반환
        return chatRoom.getChatId();
    }



}
