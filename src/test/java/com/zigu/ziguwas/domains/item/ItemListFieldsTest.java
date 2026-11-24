package com.zigu.ziguwas.domains.item;

import com.zigu.ziguwas.domains.item.dto.response.ItemListResDto;
import com.zigu.ziguwas.domains.item.dto.response.ItemResDto;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.search.dto.response.ItemSearchResDto;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** W-22: 목록·검색·상세 응답에 createdAt·viewCount 가 담긴다(기존 필드는 그대로). */
class ItemListFieldsTest {

    private final LocalDateTime created = LocalDateTime.of(2026, 11, 24, 10, 30);

    private Item item(Long viewCount) {
        User owner = User.builder().id(10L).email("a@handong.ac.kr").nickname("n")
                .univ(new University(1L, "한동대", "handong.ac.kr")).build();
        Item item = Item.builder().id(1L).user(owner).title("t").category(ItemCategory.ELECTRONICS)
                .dayPerPrice(1000L).postType(PostType.DEMAND).viewCount(viewCount).build();
        org.springframework.test.util.ReflectionTestUtils.setField(item, "createdAt", created);
        return item;
    }

    @Test
    void 목록_검색_상세_DTO_모두_createdAt_viewCount_포함() {
        Item item = item(7L);

        ItemListResDto list = ItemListResDto.fromEntity(item, 10L);
        ItemSearchResDto search = ItemSearchResDto.fromEntity(item, 10L);
        ItemResDto detail = ItemResDto.fromEntity(item, 10L);

        assertEquals(created, list.getCreatedAt());
        assertEquals(7L, list.getViewCount());
        assertEquals(created, search.getCreatedAt());
        assertEquals(7L, search.getViewCount());
        assertEquals(created, detail.getCreatedAt());
        assertEquals(7L, detail.getViewCount());
        assertEquals("DEMAND", list.getPostType()); // 기존 필드 불변
    }

    @Test
    void 조회수가_null_인_옛_데이터는_0으로() {
        assertEquals(0L, ItemListResDto.fromEntity(item(null), 10L).getViewCount());
    }
}
