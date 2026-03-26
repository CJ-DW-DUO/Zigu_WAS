package com.zigu.ziguwas.domains.chat.controller;

import com.zigu.ziguwas.domains.chat.dto.request.ChatMessageReqDto;
import com.zigu.ziguwas.domains.chat.dto.request.ChatRecvIdReqDto;
import com.zigu.ziguwas.domains.chat.service.ChatService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
//@RequestMapping("/api/v1/chat-messages")
@RequestMapping
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    /**
     * 채팅방(미리보기) 목록 조회 API
     *
     * 채팅방 목록에 표시될 정보를 가져옵니다.
     *
     * @param customUserDetails 사용자 로그인 정보
     * @return 채팅방 목록
     */
    @GetMapping("/api/v1/chatrooms")
    public ResponseEntity<?> getChatroomsPreview(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        return ResponseEntity.ok(chatService.getChatroomsPreview(customUserDetails));
    }


    /**
     * 1대1 채팅방 생성 API
     *
     * @param customUserDetails 채팅 생성자 로그인정보
     * @param dto 채팅 참여자
     * @return 채팅팡 URI
     */
    @PostMapping("/api/v1/chatrooms")
    public ResponseEntity<?> createChatroom(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody ChatRecvIdReqDto dto
    ){
        Long created = chatService.createChatRoom(customUserDetails, dto.getRecieverId());

        if(created == null) {
            throw new CustomException(ErrorCode.CHATROOM_NOT_CREATED);
        }

        return ResponseEntity.created(URI.create("/api/v1/chatrooms/" + created)).build();
    }


    /**
     * 메시지 전송 STOMP
     *
     * 매개변수의 @PathVariable 대신 @DestinationVariable로 대체
     * dto 앞의 @RequestBody 부분은 제거해야함
     *
     * @param chatRoomId 채팅방 ID
     * @param dto 채팅메시지
     * @param headerAccessor 세션정보
     */
    @MessageMapping("/chat/v1/chatrooms/{chatRoomId}")
    public void message(
            @DestinationVariable Long chatRoomId,
            ChatMessageReqDto dto,
            StompHeaderAccessor headerAccessor
    ){
        // 1. 이메일 추출
        String email = (String) headerAccessor.getSessionAttributes().get("userEmail");

        if(email == null){
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 메시지 저장 임시
         chatService.saveMessage(
                 dto.getMessage(),
                 email,
                 chatRoomId
         );

        // 2. /sub/chat/room/{roomId}를 구독중인 사용자들에게 메시지 전달
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatRoomId, dto);
    }


}
