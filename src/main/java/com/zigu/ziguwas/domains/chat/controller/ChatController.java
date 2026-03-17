package com.zigu.ziguwas.domains.chat.controller;

import com.zigu.ziguwas.domains.chat.dto.request.ChatMessageReqDto;
import com.zigu.ziguwas.domains.chat.service.ChatService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
//@RequestMapping("/api/v1/chat-messages")
@RequestMapping
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

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
