package com.zigu.ziguwas.domains.chat;

import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.chat.dto.request.CreateChatRoomReqDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomItemAndTradeInfoResDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomPreviewResDto;
import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatMessageRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.chat.service.ChatService;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.item.entity.ItemStatus;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.notification.entity.NotificationType;
import com.zigu.ziguwas.domains.notification.repository.NotificationRepository;
import com.zigu.ziguwas.domains.notification.service.NotificationService;
import com.zigu.ziguwas.domains.notification.service.PushNotificationService;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyRegisteredItemResDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** S0 K3(W-03·W-06·W-16·W-17·W-18) 최소 검증. 외부 저장소는 모두 목킹한다. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMypageS0Test {

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

    University univ = new University(1L, "한동대", "handong.ac.kr");
    User author = user(10L, "author@handong.ac.kr");
    User other = user(20L, "other@handong.ac.kr");
    User third = user(30L, "third@handong.ac.kr");
    Item supply;
    Item demand;

    static User user(Long id, String email) {
        return User.builder().id(id).email(email).nickname("n" + id)
                .univ(new University(1L, "한동대", "handong.ac.kr")).build();
    }

    static CustomUserDetails details(User u) {
        return new CustomUserDetails(u);
    }

    @BeforeEach
    void setUp() {
        supply = Item.builder().id(1L).user(author).title("s").category(ItemCategory.ELECTRONICS)
                .dayPerPrice(1000L).postType(PostType.SUPPLY).build();
        demand = Item.builder().id(2L).user(author).title("d").category(ItemCategory.ELECTRONICS)
                .dayPerPrice(1000L).postType(PostType.DEMAND).build();
        for (User u : List.of(author, other, third)) {
            when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
            when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(itemRepository.findById(1L)).thenReturn(Optional.of(supply));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(demand));
        when(chatParticipantRepository.findAllByUserId(anyLong())).thenReturn(List.of());
        when(chatRoomRepository.save(any())).thenReturn(ChatRoom.builder().id("room1").itemId(1L).build());
    }

    private CreateChatRoomReqDto req(Long receiverId, Long itemId) {
        return CreateChatRoomReqDto.builder().receiverId(receiverId).itemId(itemId).build();
    }

    // W-03 — FE 정상 흐름: SUPPLY·DEMAND 모두 "요청자 → 게시글 작성자" 로 방을 만든다.
    @Test
    void createChatRoom_작성자에게_보내는_정상흐름은_SUPPLY_DEMAND_모두_통과() {
        assertEquals("room1", chatService.createChatRoom(details(other), req(author.getId(), 1L)));
        assertEquals("room1", chatService.createChatRoom(details(other), req(author.getId(), 2L)));
    }

    // W-03 — 작성자가 아닌 사용자를 receiverId 로 지정하거나 본인을 지정하면 거부.
    @Test
    void createChatRoom_작성자가_아니거나_본인이면_거부() {
        CustomException notAuthor = assertThrows(CustomException.class,
                () -> chatService.createChatRoom(details(other), req(third.getId(), 1L)));
        assertEquals(ErrorCode.INVALID_CHAT_RECEIVER, notAuthor.getErrorCode());

        CustomException selfOfOther = assertThrows(CustomException.class,
                () -> chatService.createChatRoom(details(other), req(other.getId(), 1L)));
        assertEquals(ErrorCode.INVALID_CHAT_RECEIVER, selfOfOther.getErrorCode());

        // 작성자 본인이 자기 글에 receiverId=자기 로 요청
        CustomException authorSelf = assertThrows(CustomException.class,
                () -> chatService.createChatRoom(details(author), req(author.getId(), 1L)));
        assertEquals(ErrorCode.INVALID_CHAT_RECEIVER, authorSelf.getErrorCode());
    }

    // W-16 — 거래정보 imageUrl 은 ItemImage.toString() 이 아니라 실제 URL.
    @Test
    void itemTradeInfo_imageUrl은_실제_URL() {
        supply.getImageUrl().add(new ItemImage("http://s3/real.png", true));
        ChatRoom room = ChatRoom.builder().id("room1").itemId(1L).build();
        when(chatRoomRepository.findById("room1")).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull("room1", other.getId())).thenReturn(true);
        when(chatParticipantRepository.findAllByChatRoomId("room1")).thenReturn(List.of(
                ChatParticipant.builder().chatRoomId("room1").userId(author.getId()).build(),
                ChatParticipant.builder().chatRoomId("room1").userId(other.getId()).build()));

        ChatRoomItemAndTradeInfoResDto res = chatService.getChatroomItemAndTradeInfo(details(other), "room1");

        assertEquals("http://s3/real.png", res.getImageUrl());
    }

    // W-17 — 메시지 0건 방은 목록에서 제외, 메시지 있는 방은 유지.
    @Test
    void chatroomsPreview_메시지없는_방은_제외() {
        ChatParticipant emptyP = ChatParticipant.builder().chatRoomId("empty").userId(other.getId()).build();
        ChatParticipant fullP = ChatParticipant.builder().chatRoomId("full").userId(other.getId()).build();
        when(chatParticipantRepository.findAllByUserIdAndLeftAtIsNull(other.getId())).thenReturn(List.of(emptyP, fullP));
        for (String id : List.of("empty", "full")) {
            when(chatRoomRepository.findById(id)).thenReturn(Optional.of(ChatRoom.builder().id(id).itemId(1L).build()));
            when(chatParticipantRepository.findAllByChatRoomId(id)).thenReturn(List.of(
                    ChatParticipant.builder().chatRoomId(id).userId(author.getId()).build(),
                    ChatParticipant.builder().chatRoomId(id).userId(other.getId()).build()));
        }
        when(chatMessageRepository.findFirstByChatRoomIdAndTimestampAfterOrderByTimestampDesc(anyString(), any()))
                .thenReturn(Optional.empty());
        when(chatMessageRepository.findFirstByChatRoomIdAndTimestampAfterOrderByTimestampDesc(org.mockito.ArgumentMatchers.eq("full"), any()))
                .thenReturn(Optional.of(ChatMessage.builder().chatRoomId("full").message("hi").timestamp(LocalDateTime.now()).build()));

        List<ChatRoomPreviewResDto> rooms = chatService.getChatroomsPreview(details(other));

        assertEquals(1, rooms.size());
        assertEquals("full", rooms.get(0).getRoomId());
    }

    // W-06 — 탈퇴한(조회 안 되는) 수신자는 예외 없이 건너뛰고 저장·푸시하지 않는다.
    @Test
    void createNotification_탈퇴_수신자는_건너뜀() {
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
        PushNotificationService push = org.mockito.Mockito.mock(PushNotificationService.class);
        NotificationService service = new NotificationService(notifications, users,
                org.mockito.Mockito.mock(ChatParticipantRepository.class), push);
        when(users.findById(99L)).thenReturn(Optional.empty());

        service.createNotification(new NotificationCreatedEvent(99L, NotificationType.CHAT, "t", "c", "room1", 1L, "i"));

        verify(notifications, never()).save(any());
        verify(push, never()).send(any());
    }

    // W-18 — 응답에 itemStatus·itemCategory 가 추가된다(RENTING 도 변환 가능).
    @Test
    void myRegisteredItem_상태와_카테고리_포함() {
        Item renting = Item.builder().id(3L).user(author).title("r").category(ItemCategory.BOOKS_AND_ACADEMICS)
                .dayPerPrice(500L).postType(PostType.SUPPLY).itemStatus(ItemStatus.RENTING).build();

        MyRegisteredItemResDto dto = MyRegisteredItemResDto.fromEntity(renting);

        assertEquals("RENTING", dto.getItemStatus());
        assertEquals("BOOKS_AND_ACADEMICS", dto.getItemCategory());
    }
}
