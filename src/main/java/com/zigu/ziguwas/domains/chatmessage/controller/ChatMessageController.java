package com.zigu.ziguwas.domains.chatmessage.controller;

import com.zigu.ziguwas.domains.chatmessage.dto.request.ChatMessageReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
//@RequestMapping("/api/v1/chat-messages")
@RequestMapping
public class ChatMessageController {

    private final SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat/message")
    public void message(ChatMessageReqDto dto){
        // 1. 메시지 저장 임시
        // chatMessageService.saveMessage(dto);

        // 2. /sub/chat/room/{roomId}를 구독중인 사용자들에게 메시지 전달
        messagingTemplate.convertAndSend("/sub/chat/room/" + dto.getRoomId(), dto);
    }


}
