package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.item.event.ItemImagesDeletedEvent;
import com.zigu.ziguwas.domains.item.repository.ItemListRepository;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.user.dto.mypage.request.MyPageProfileReqDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.MyPageService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** S0 W-11: 프로필 사진 교체는 '새 업로드 성공 → DB 갱신 → (커밋 후 리스너가) 이전 객체 삭제'. */
class ProfileImageOrderTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final S3Service s3Service = mock(S3Service.class);
    private final User user = mock(User.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final MyPageService service =
            new MyPageService(userRepository, mock(ItemRepository.class), mock(ItemListRepository.class), s3Service, eventPublisher);
    private final MockMultipartFile image = new MockMultipartFile("profileImage", "a.png", "image/png", new byte[]{1});
    private final MyPageProfileReqDto req = new MyPageProfileReqDto();

    @BeforeEach
    void setUp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.getProfilePhotoUrl()).thenReturn("https://b.s3/old.png");
    }

    @Test
    void 업로드_실패하면_이전_이미지_삭제_이벤트가_발행되지_않는다() {
        when(s3Service.uploadSingleFile(any())).thenThrow(new CustomException(ErrorCode.FILE_UPLOAD_FAIL));

        assertThrows(CustomException.class, () -> service.updateProfile(1L, req, image));

        verify(user, never()).updateProfileImage(any());
        verify(s3Service, never()).deleteFile(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 업로드_성공_후_DB갱신_다음에_이전_이미지_삭제_이벤트_발행() {
        when(s3Service.uploadSingleFile(any())).thenReturn("https://b.s3/new.png");

        service.updateProfile(1L, req, image);

        InOrder order = inOrder(s3Service, user, eventPublisher);
        order.verify(s3Service).uploadSingleFile(any());
        order.verify(user).updateProfileImage("https://b.s3/new.png");
        order.verify(eventPublisher).publishEvent(new ItemImagesDeletedEvent(List.of("https://b.s3/old.png")));
        verify(s3Service, never()).deleteFile(any()); // S3 삭제는 커밋 뒤 리스너 몫
    }

    @Test
    void 이전_이미지가_없으면_삭제_이벤트를_발행하지_않는다() {
        when(user.getProfilePhotoUrl()).thenReturn(null);
        when(s3Service.uploadSingleFile(any())).thenReturn("https://b.s3/new.png");

        service.updateProfile(1L, req, image);

        verify(user).updateProfileImage("https://b.s3/new.png");
        verifyNoInteractions(eventPublisher);
    }
}
