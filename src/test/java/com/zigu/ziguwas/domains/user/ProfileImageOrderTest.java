package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.item.repository.ItemListRepository;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.user.dto.mypage.request.MyPageProfileReqDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.MyPageService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** S0 W-11: 프로필 사진 교체는 '새 업로드 성공 → DB 갱신 → (커밋 후) 이전 객체 삭제'. */
class ProfileImageOrderTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final S3Service s3Service = mock(S3Service.class);
    private final User user = mock(User.class);
    private final MyPageService service =
            new MyPageService(userRepository, mock(ItemRepository.class), mock(ItemListRepository.class), s3Service);
    private final MockMultipartFile image = new MockMultipartFile("profileImage", "a.png", "image/png", new byte[]{1});
    private final MyPageProfileReqDto req = new MyPageProfileReqDto();

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.getProfilePhotoUrl()).thenReturn("https://b.s3/old.png");
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void 업로드_실패하면_이전_이미지는_지워지지_않는다() {
        when(s3Service.uploadSingleFile(any())).thenThrow(new CustomException(ErrorCode.FILE_UPLOAD_FAIL));

        assertThrows(CustomException.class, () -> service.updateProfile(1L, req, image));

        verify(s3Service, never()).deleteFile(any());
        verify(user, never()).updateProfileImage(any());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void 업로드_성공_후_DB갱신_커밋_뒤에_이전_이미지_삭제() {
        when(s3Service.uploadSingleFile(any())).thenReturn("https://b.s3/new.png");

        service.updateProfile(1L, req, image);

        InOrder order = inOrder(s3Service, user);
        order.verify(s3Service).uploadSingleFile(any());
        order.verify(user).updateProfileImage("https://b.s3/new.png");
        verify(s3Service, never()).deleteFile(any()); // 아직 커밋 전

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(s3Service).deleteFile("https://b.s3/old.png");
    }

    @Test
    void 이전_이미지_삭제가_실패해도_예외로_번지지_않는다() {
        when(s3Service.uploadSingleFile(any())).thenReturn("https://b.s3/new.png");
        doThrow(new CustomException(ErrorCode.FAIL_DELETE_FILE)).when(s3Service).deleteFile(any());

        service.updateProfile(1L, req, image);

        assertDoesNotThrow(() ->
                TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit));
    }
}
