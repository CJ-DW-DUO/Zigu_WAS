package com.zigu.ziguwas.domains.chat.service;

import com.zigu.ziguwas.domains.chat.dto.request.ChatMessageReqDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 실시간 메시지 저장
     *
     * @param dto 메시지 내용
     * @param email 이메일
     */
    @Transactional
    public void saveMessage(ChatMessageReqDto dto, String email) {
        // 1. 발신자 정보 조회
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 해당 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        // 3. 참여자 권한 검증 (선택 사항: 보낸 사람이 해당 방의 참여자인지 확인)
        validateParticipant(chatRoom, sender);

        // 4. 메시지 엔티티 생성 및 저장
        ChatMessage chatMessage = new ChatMessage(
                null,
                chatRoom,
                sender,
                dto.getMessage(),
                "" // 이미지 URL 필드는 필요 시 추가 처리
        );

        chatMessageRepository.save(chatMessage);
    }

    /**
     * 채팅방 생성 및 참여자 등록 (거래 게시글 등에서 채팅 시작 시 호출)
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
