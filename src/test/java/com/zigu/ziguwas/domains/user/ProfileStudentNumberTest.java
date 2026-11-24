package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.item.repository.ItemListRepository;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.dto.mypage.request.MyPageProfileReqDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.MyPageService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 프로필 수정의 학번은 값이 바뀔 때만 7~10자리 숫자를 검사한다. (화면이 사진만 바꿔도 현재 학번을 다시 보내기 때문) */
class ProfileStudentNumberTest {

    UserRepository userRepository = mock(UserRepository.class);
    MyPageService service = new MyPageService(userRepository, mock(ItemRepository.class), mock(ItemListRepository.class),
            mock(S3Service.class), mock(ApplicationEventPublisher.class));
    User user;

    @BeforeEach
    void setUp() {
        // 예전 형식(2자리)으로 저장된 학번을 가진 사용자
        user = User.builder().id(1L).email("a@snu.ac.kr").nickname("홍길동").studentNumber("26")
                .univ(new University(1L, "서울대학교", "snu.ac.kr")).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    private MyPageProfileReqDto req(String nickname, String studentNumber) {
        return MyPageProfileReqDto.builder().nickname(nickname).studentNumber(studentNumber).build();
    }

    // 저장된 학번을 그대로 다시 보내면(예전 형식이어도) 다른 항목 수정이 막히지 않는다.
    @Test
    void 학번이_그대로면_예전_형식이어도_검사하지_않는다() {
        service.updateProfile(1L, req("김철수", "26"), null);

        assertEquals("김철수", user.getNickname());
        assertEquals("26", user.getStudentNumber());
    }

    // 값이 바뀌면 7~10자리 숫자만 허용한다. 거절되면 다른 항목도 바뀌지 않는다.
    @Test
    void 학번이_바뀌면_7에서_10자리_숫자만_허용() {
        for (String bad : new String[]{"27", "123456", "12345678901", "abc1234567"}) {
            CustomException e = assertThrows(CustomException.class, () -> service.updateProfile(1L, req("김철수", bad), null));
            assertEquals(ErrorCode.INVALID_STUDENT_NUMBER, e.getErrorCode());
        }
        assertEquals("홍길동", user.getNickname());

        service.updateProfile(1L, req(null, "20261234"), null);
        assertEquals("20261234", user.getStudentNumber());
    }

    // 학번을 보내지 않으면(null) 그대로 둔다.
    @Test
    void 학번을_보내지_않으면_그대로() {
        service.updateProfile(1L, req("김철수", null), null);

        assertEquals("26", user.getStudentNumber());
    }
}
