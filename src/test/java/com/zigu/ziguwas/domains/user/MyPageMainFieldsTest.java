package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyPageMainResDto;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyPageProfileResDto;
import com.zigu.ziguwas.domains.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 마이페이지 응답에 추가된 필드(학번·이메일·요청글 이미지)와 기존 필드 유지 확인. */
class MyPageMainFieldsTest {

    User user = User.builder().id(1L).email("zigu@snu.ac.kr").nickname("홍길동").studentNumber("20261234")
            .univ(new University(1L, "서울대학교", "snu.ac.kr")).build();

    private Item item(Long id, PostType postType, String imageUrl) {
        Item item = Item.builder().id(id).user(user).title("t").category(ItemCategory.ELECTRONICS)
                .dayPerPrice(1000L).postType(postType).build();
        item.getImageUrl().add(new ItemImage(imageUrl, true));
        return item;
    }

    @Test
    void 메인_응답에_학번_이메일_요청글이_추가되고_기존_필드는_그대로() {
        List<Item> registered = List.of(item(3L, PostType.DEMAND, "d3"), item(2L, PostType.SUPPLY, "s2"));
        List<Item> demand = List.of(item(3L, PostType.DEMAND, "d3"));

        MyPageMainResDto dto = MyPageMainResDto.fromEntity(user, registered, demand);

        assertEquals("20261234", dto.getStudentNumber());
        assertEquals("zigu@snu.ac.kr", dto.getEmail());
        assertEquals(1, dto.getDemandItemsMainImage().size());
        assertEquals("d3", dto.getDemandItemsMainImage().get(0).getMainImageUrl());
        // 기존 필드 의미는 그대로: 공급글·요청글이 섞인 최신 목록
        assertEquals("홍길동", dto.getNickname());
        assertEquals("서울대학교", dto.getUnivName());
        assertEquals(2, dto.getRegisteredItemsMainImage().size());
    }

    @Test
    void 프로필_응답에_이메일이_추가된다() {
        MyPageProfileResDto dto = MyPageProfileResDto.fromEntity(user);

        assertEquals("zigu@snu.ac.kr", dto.getEmail());
        assertEquals("20261234", dto.getStudentNumber());
        assertTrue(dto.getNickname().equals("홍길동"));
    }
}
