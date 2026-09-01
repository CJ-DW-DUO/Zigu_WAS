package com.zigu.ziguwas.domains.chat.controller;

import com.zigu.ziguwas.domains.chat.api.ChatApi;
import com.zigu.ziguwas.domains.chat.dto.request.ChatMessageReqDto;
import com.zigu.ziguwas.domains.chat.dto.request.CreateChatRoomReqDto;
import com.zigu.ziguwas.domains.chat.service.ChatService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequiredArgsConstructor
//@RequestMapping("/api/v1/chat-messages")
@RequestMapping
public class ChatController implements ChatApi {

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
     * 채팅방 상세조회 API
     *
     * 채팅방 내에서 진행한 대화내역등을 모두 볼 수 있다.
     *
     * @param customUserDetails 로그인정보
     * @param chatRoomId 채팅방ID
     * @return 채팅정보
     */
    @GetMapping("/api/v1/chatrooms/{chatRoomId}")
    public ResponseEntity<?> getChatroomDetail(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable String chatRoomId,
            @RequestParam Integer page,
            @RequestParam Integer size
    ){
        return ResponseEntity.ok(chatService.getChatroomDetail(customUserDetails, chatRoomId, page, size));
    }

    /**
     * 채팅방 거래 정보 조회 API
     *
     * 해당 채팅방이 어떤 물품에 대한 채팅방인지, 거래를 진행중인지 조회한다.
     *
     * @param customUserDetails 로그인정보
     * @param chatRoomId 채팅방ID
     * @return 채팅방 물품 및 거래정보
     */
    @GetMapping("/api/v1/chatrooms/{chatRoomId}/item-trade-info")
    public ResponseEntity<?> getChatroomItemInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable String chatRoomId
    ){
        return ResponseEntity.ok(chatService.getChatroomItemAndTradeInfo(customUserDetails, chatRoomId));
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
            @RequestBody CreateChatRoomReqDto dto
    ){
        String created = chatService.createChatRoom(customUserDetails, dto);

        if(created == null) {
            throw new CustomException(ErrorCode.CHATROOM_NOT_CREATED);
        }

        return ResponseEntity.created(URI.create("/api/v1/chatrooms/" + created)).build();
    }


    /**
     * 채팅 이미지 업로드 API
     *
     * 이미지를 S3에 업로드하고 URL만 반환한다. 실제 채팅 메시지 전송은
     * 이 URL을 담아 기존 STOMP 전송 경로(/pub/chat/v1/chatrooms/{chatRoomId})로 별도 진행해야 한다.
     *
     * @param customUserDetails 로그인정보
     * @param chatRoomId 채팅방 ID
     * @param image 업로드할 이미지 파일
     * @return 업로드된 이미지 URL
     */
    @PostMapping(value = "/api/v1/chatrooms/{chatRoomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadChatImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable String chatRoomId,
            @RequestPart("image") MultipartFile image
    ){
        return ResponseEntity.ok(chatService.uploadChatImage(customUserDetails, chatRoomId, image));
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
            @DestinationVariable String chatRoomId,
            ChatMessageReqDto dto,
            StompHeaderAccessor headerAccessor
    ){
        String email = (String) headerAccessor.getSessionAttributes().get("userEmail");

        if (email == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        chatService.branchMessage(chatRoomId, dto, email);
    }

    /**
     * 채팅방 나가기 API
     *
     * 채팅방을 실제로 삭제하는 것이 아니라 요청한 사용자의 목록에서만 숨긴다.
     * 상대방이 새 메시지를 보내면 채팅방이 다시 나타나며, 이때 나가기 이전의 대화는 보이지 않는다.
     *
     * @param customUserDetails 로그인정보
     * @param chatRoomId 채팅방ID
     * @return 응답 본문 없음
     */
    @DeleteMapping("/api/v1/chatrooms/{chatRoomId}")
    public ResponseEntity<?> leaveChatroom(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable String chatRoomId
    ){
        chatService.leaveChatRoom(customUserDetails, chatRoomId);
        return ResponseEntity.noContent().build();
    }


}
