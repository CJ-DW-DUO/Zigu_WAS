package com.zigu.ziguwas.domains.chat;

import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.chat.dto.request.CreateChatRoomReqDto;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.chat.service.ChatService;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 채팅방 생성 차단은 거래용 BLOCKED_USER_ACCESS 가 아니라 채팅 전용 BLOCKED_USER_CHAT 으로 응답한다(FE 문구 매핑 분리). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatBlockCodeTest {

    @Mock UserRepository userRepository;
    @Mock ItemRepository itemRepository;
    @Mock BlockRepository blockRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @InjectMocks ChatService chatService;

    private static User user(Long id, String email) {
        return User.builder().id(id).email(email).nickname("n" + id)
                .univ(new University(1L, "한동대", "handong.ac.kr")).build();
    }

    @Test
    void 차단_관계면_채팅_전용_코드로_채팅방_생성을_막는다() {
        User author = user(10L, "author@handong.ac.kr");
        User other = user(20L, "other@handong.ac.kr");
        Item item = Item.builder().id(1L).user(author).title("s").category(ItemCategory.ELECTRONICS)
                .dayPerPrice(1000L).postType(PostType.SUPPLY).build();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(blockRepository.existsBlockBetween(other.getId(), author.getId())).thenReturn(true);

        CustomException e = assertThrows(CustomException.class, () -> chatService.createChatRoom(
                new CustomUserDetails(other),
                CreateChatRoomReqDto.builder().receiverId(author.getId()).itemId(1L).build()));

        assertEquals(ErrorCode.BLOCKED_USER_CHAT, e.getErrorCode());
        verifyNoInteractions(chatRoomRepository);
    }
}
