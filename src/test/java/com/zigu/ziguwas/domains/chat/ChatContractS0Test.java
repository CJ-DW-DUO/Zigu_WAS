package com.zigu.ziguwas.domains.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.chat.dto.ChatSendInfoDto;
import com.zigu.ziguwas.domains.chat.dto.request.ChatMessageReqDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomPreviewResDto;
import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatMessageRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.chat.scheduler.ChatRoomRetentionScheduler;
import com.zigu.ziguwas.domains.chat.service.ChatService;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import com.zigu.ziguwas.websocket.StompErrorHandler;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** S0+ K7(W-10·W-24·W-25·W-26) 최소 검증. 외부 저장소는 모두 목킹한다. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatContractS0Test {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatParticipantRepository chatParticipantRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TradeRepository tradeRepository;
    @Mock ItemRepository itemRepository;
    @Mock BlockRepository blockRepository;
    @Mock SimpMessageSendingOperations messagingTemplate;
    @Mock S3Service s3Service;
    @InjectMocks ChatService chatService;

    User author = user(10L, "author@handong.ac.kr");
    User other = user(20L, "other@handong.ac.kr");
    Item item;

    static User user(Long id, String email) {
        return User.builder().id(id).email(email).nickname("n" + id)
                .univ(new University(1L, "한동대", "handong.ac.kr")).build();
    }

    @BeforeEach
    void setUp() {
        item = Item.builder().id(1L).user(author).title("노트북").category(ItemCategory.ELECTRONICS)
                .dayPerPrice(1000L).postType(PostType.SUPPLY).build();
        for (User u : List.of(author, other)) {
            when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
            when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(chatRoomRepository.findById("room1")).thenReturn(Optional.of(ChatRoom.builder().id("room1").itemId(1L).build()));
        when(chatParticipantRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull("room1", other.getId())).thenReturn(true);
        when(chatParticipantRepository.findAllByChatRoomId("room1")).thenReturn(List.of(
                ChatParticipant.builder().chatRoomId("room1").userId(author.getId()).build(),
                ChatParticipant.builder().chatRoomId("room1").userId(other.getId()).build()));
    }

    // W-24 — 목록에 마지막 메시지 ID·발신자·물품·상대 ID·제목·썸네일이 담긴다.
    @Test
    void chatroomsPreview_메타_필드_포함() {
        item.getImageUrl().add(new ItemImage("http://s3/sub.png", false));
        item.getImageUrl().add(new ItemImage("http://s3/main.png", true));
        when(chatParticipantRepository.findAllByUserIdAndLeftAtIsNull(other.getId())).thenReturn(List.of(
                ChatParticipant.builder().chatRoomId("room1").userId(other.getId()).build()));
        when(chatMessageRepository.findFirstByChatRoomIdAndTimestampAfterOrderByTimestampDesc(eq("room1"), any()))
                .thenReturn(Optional.of(ChatMessage.builder().id("m1").chatRoomId("room1").senderId(author.getId())
                        .message("hi").timestamp(LocalDateTime.now()).build()));

        ChatRoomPreviewResDto dto = chatService.getChatroomsPreview(new CustomUserDetails(other)).get(0);

        assertEquals("m1", dto.getLastMessageId());
        assertEquals(author.getId(), dto.getLastSenderId());
        assertEquals(1L, dto.getItemId());
        assertEquals(author.getId(), dto.getReceiverId());
        assertEquals("노트북", dto.getItemTitle());
        assertEquals("http://s3/main.png", dto.getItemImageUrl());
    }

    // W-24 — 메시지 없는 방(신청만 있는 방)·삭제된 물품은 해당 필드만 null, 목록은 깨지지 않는다.
    @Test
    void chatroomsPreview_메시지없고_물품삭제면_해당_필드만_null() {
        ChatRoom deletedItemRoom = ChatRoom.builder().id("room2").itemId(99L).build();
        when(chatRoomRepository.findById("room2")).thenReturn(Optional.of(deletedItemRoom));
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());
        when(chatParticipantRepository.findAllByChatRoomId("room2")).thenReturn(List.of(
                ChatParticipant.builder().chatRoomId("room2").userId(author.getId()).build(),
                ChatParticipant.builder().chatRoomId("room2").userId(other.getId()).build()));
        when(chatParticipantRepository.findAllByUserIdAndLeftAtIsNull(other.getId())).thenReturn(List.of(
                ChatParticipant.builder().chatRoomId("room2").userId(other.getId()).build()));
        when(chatMessageRepository.findFirstByChatRoomIdAndTimestampAfterOrderByTimestampDesc(eq("room2"), any()))
                .thenReturn(Optional.empty());
        when(tradeRepository.existsByItemIdAndRenterIdAndRenteeIdAndTradeStatusIn(eq(99L), eq(author.getId()), eq(other.getId()), any()))
                .thenReturn(true);

        ChatRoomPreviewResDto dto = chatService.getChatroomsPreview(new CustomUserDetails(other)).get(0);

        assertNull(dto.getLastMessageId());
        assertNull(dto.getLastSenderId());
        assertEquals(99L, dto.getItemId());
        assertNull(dto.getItemTitle());
        assertNull(dto.getItemImageUrl());
    }

    private ChatMessageReqDto reqWithClientId(String clientMessageId) throws Exception {
        return new ObjectMapper().readValue(
                "{\"message\":\"hello\",\"clientMessageId\":\"" + clientMessageId + "\"}", ChatMessageReqDto.class);
    }

    // W-25 — 같은 clientMessageId 재전송: 저장·알림은 1번, 전송(echo)은 매번 같은 messageId·clientMessageId 로.
    // (PG 이관 후: Redis 선점 대신 chat_message 의 (방, 보낸 사람, clientMessageId) 유니크 키로 판단)
    @Test
    void branchMessage_같은_clientMessageId_재전송은_중복저장_없이_echo() throws Exception {
        ChatMessage saved = ChatMessage.builder().id("m1").chatRoomId("room1").senderId(other.getId())
                .message("hello").timestamp(LocalDateTime.now()).clientMessageId("cm-1").build();
        when(chatMessageRepository.save(any())).thenReturn(saved);
        when(chatMessageRepository.findByChatRoomIdAndSenderIdAndClientMessageId("room1", other.getId(), "cm-1"))
                .thenReturn(Optional.empty(), Optional.of(saved));

        chatService.branchMessage("room1", reqWithClientId("cm-1"), other.getEmail());
        chatService.branchMessage("room1", reqWithClientId("cm-1"), other.getEmail());

        verify(chatMessageRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/sub/chat/room/room1"), sent.capture());
        for (Object o : sent.getAllValues()) {
            ChatSendInfoDto echo = (ChatSendInfoDto) o;
            assertEquals("m1", echo.getMessageId());
            assertEquals("cm-1", echo.getClientMessageId());
            assertEquals("hello", echo.getMessage());
        }
    }

    // W-25 — 동시 재전송: 조회 시점엔 없었지만 저장 때 유니크 키가 막으면 먼저 저장된 메시지를 echo 하고 알림은 만들지 않는다.
    @Test
    void branchMessage_동시_재전송은_유니크_충돌_뒤_기존_메시지로_echo() throws Exception {
        ChatMessage existing = ChatMessage.builder().id("m3").chatRoomId("room1").senderId(other.getId())
                .message("hello").timestamp(LocalDateTime.now()).clientMessageId("cm-3").build();
        when(chatMessageRepository.findByChatRoomIdAndSenderIdAndClientMessageId("room1", other.getId(), "cm-3"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(chatMessageRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        chatService.branchMessage("room1", reqWithClientId("cm-3"), other.getEmail());

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/chat/room/room1"), sent.capture());
        assertEquals("m3", ((ChatSendInfoDto) sent.getValue()).getMessageId());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    // W-25 — ID 없는 요청은 기존처럼 매번 저장하고, 유니크 위반이 ID 없는 요청에서 나면 그대로 던진다.
    @Test
    void branchMessage_ID없으면_매번_저장() throws Exception {
        ChatMessage saved = ChatMessage.builder().id("m2").chatRoomId("room1").senderId(other.getId())
                .message("hello").timestamp(LocalDateTime.now()).build();
        when(chatMessageRepository.save(any())).thenReturn(saved);
        ChatMessageReqDto noId = new ObjectMapper().readValue("{\"message\":\"hello\"}", ChatMessageReqDto.class);

        chatService.branchMessage("room1", noId, other.getEmail());
        chatService.branchMessage("room1", noId, other.getEmail());

        verify(chatMessageRepository, times(2)).save(any());
        verify(chatMessageRepository, never()).findByChatRoomIdAndSenderIdAndClientMessageId(anyString(), anyLong(), anyString());
    }

    // W-26 — 인증 예외는 ERROR 프레임의 message 헤더·본문에 ErrorDto 형태로 담긴다. 모르는 예외는 기본 동작.
    @Test
    void stompErrorHandler_에러코드와_JWT_예외를_프레임에_싣는다() throws Exception {
        StompErrorHandler handler = new StompErrorHandler();

        Message<byte[]> tokenMissing = handler.handleClientMessageProcessingError(null,
                new MessageDeliveryException(null, "Failed to send", new CustomException(ErrorCode.TOKEN_NOT_FOUND)));
        assertEquals(ErrorCode.TOKEN_NOT_FOUND.getMessage(), StompHeaderAccessor.wrap(tokenMissing).getMessage());
        assertEquals("{\"status\":403,\"message\":\"" + ErrorCode.TOKEN_NOT_FOUND.getMessage() + "\"}",
                new String(tokenMissing.getPayload(), StandardCharsets.UTF_8));

        Message<byte[]> expired = handler.handleClientMessageProcessingError(null,
                new MessageDeliveryException(null, "Failed to send", new ExpiredJwtException(null, null, "expired")));
        assertEquals(ErrorCode.JWT_TOKEN_PARSING_ERROR.getMessage(), StompHeaderAccessor.wrap(expired).getMessage());
        assertEquals("{\"status\":401,\"message\":\"" + ErrorCode.JWT_TOKEN_PARSING_ERROR.getMessage() + "\"}",
                new String(expired.getPayload(), StandardCharsets.UTF_8));

        Message<byte[]> unknown = handler.handleClientMessageProcessingError(null, new IllegalStateException("boom"));
        assertEquals("boom", StompHeaderAccessor.wrap(unknown).getMessage());
        assertEquals(0, unknown.getPayload().length);
    }

    // W-10 — 보관 배치는 S3 를 건드리지 않고(의존성 자체가 없음) 방·메시지·참여자만 지운다.
    @Test
    void retentionScheduler_S3_없이_방_메시지_참여자만_삭제() {
        ChatRoomRepository rooms = mock(ChatRoomRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        ChatParticipantRepository participants = mock(ChatParticipantRepository.class);
        ChatRoomRetentionScheduler scheduler = new ChatRoomRetentionScheduler(rooms, messages, participants);
        ReflectionTestUtils.setField(scheduler, "retentionDays", 90);
        when(rooms.findByClosedAtBefore(any(), any())).thenReturn(List.of(ChatRoom.builder().id("old").itemId(1L).build()));
        when(messages.countByChatRoomId("old")).thenReturn(3L);

        scheduler.deleteExpiredChatRooms();

        verify(messages).deleteAllByChatRoomId("old");
        verify(participants).deleteAllByChatRoomId("old");
        verify(rooms).deleteById("old");
        verify(messages, never()).findByChatRoomIdAndImageUrlNotNull(anyString());
    }
}
