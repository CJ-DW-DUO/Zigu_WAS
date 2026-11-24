package com.zigu.ziguwas.domains.item;

import com.zigu.ziguwas.domains.item.dto.response.ItemResDto;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.entity.Gender;
import com.zigu.ziguwas.domains.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 게시글 상세 응답의 작성자 학번·성별: 학번은 항상, 성별은 작성자가 공개로 둔 경우에만. */
class ItemWriterInfoTest {

    private Item itemBy(boolean genderVisible) {
        User writer = User.builder().id(1L).email("a@snu.ac.kr").nickname("작성자").studentNumber("20261234")
                .gender(Gender.FEMALE).genderVisible(genderVisible)
                .univ(new University(1L, "서울대학교", "snu.ac.kr")).build();
        return Item.builder().id(10L).user(writer).title("t").category(ItemCategory.ELECTRONICS).dayPerPrice(1000L).build();
    }

    @Test
    void 성별을_공개한_작성자는_성별이_내려간다() {
        ItemResDto dto = ItemResDto.fromEntity(itemBy(true), 2L);

        assertEquals("20261234", dto.getStudentNumber());
        assertEquals(Gender.FEMALE, dto.getGender());
    }

    @Test
    void 성별을_숨긴_작성자는_학번만_내려가고_성별은_null() {
        ItemResDto dto = ItemResDto.fromEntity(itemBy(false), 2L);

        assertEquals("20261234", dto.getStudentNumber());
        assertNull(dto.getGender());
    }
}
