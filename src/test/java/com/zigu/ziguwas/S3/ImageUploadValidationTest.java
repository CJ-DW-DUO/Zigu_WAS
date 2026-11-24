package com.zigu.ziguwas.S3;

import com.amazonaws.services.s3.AmazonS3;
import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemImageRepository;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.item.service.ItemService;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** W-13: 이미지 업로드는 허용된 형식(Content-Type·확장자·시그니처)만, 한 번에 5장까지. */
class ImageUploadValidationTest {

    AmazonS3 amazonS3 = mock(AmazonS3.class);
    S3Service s3Service = new S3Service(amazonS3);

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    static final byte[] GIF = "GIF89a......".getBytes(StandardCharsets.US_ASCII);
    static final byte[] WEBP = "RIFF....WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);
    static final byte[] HEIC = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c'};
    static final byte[] HTML = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() throws Exception {
        when(amazonS3.getUrl(any(), anyString())).thenReturn(new URL("https://bucket.s3.example/x.jpg"));
    }

    private MockMultipartFile file(String name, String type, byte[] bytes) {
        return new MockMultipartFile("images", name, type, bytes);
    }

    private ErrorCode errorOf(MockMultipartFile f) {
        return assertThrows(CustomException.class, () -> s3Service.uploadSingleFile(f)).getErrorCode();
    }

    @Test
    void 허용된_이미지_형식은_통과() {
        assertDoesNotThrow(() -> s3Service.uploadSingleFile(file("a.jpg", "image/jpeg", JPEG)));
        assertDoesNotThrow(() -> s3Service.uploadSingleFile(file("a.PNG", "image/png", PNG)));
        assertDoesNotThrow(() -> s3Service.uploadSingleFile(file("a.gif", "image/gif", GIF)));
        assertDoesNotThrow(() -> s3Service.uploadSingleFile(file("a.webp", "image/webp", WEBP)));
        assertDoesNotThrow(() -> s3Service.uploadSingleFile(file("a.heic", "image/heic", HEIC)));
        assertDoesNotThrow(() -> s3Service.uploadSingleFile(file("noext", "image/jpeg", JPEG))); // 확장자 없으면 기존처럼 jpeg 로 저장
    }

    @Test
    void 이미지가_아닌_파일은_거부() {
        assertEquals(ErrorCode.INVALID_FILE_TYPE, errorOf(file("a.html", "text/html", HTML)));
        assertEquals(ErrorCode.INVALID_FILE_TYPE, errorOf(file("a.jpg", null, JPEG)));             // Content-Type 없음
        assertEquals(ErrorCode.INVALID_FILE_TYPE, errorOf(file("a.html", "image/jpeg", JPEG)));    // 확장자가 이미지가 아님
        assertEquals(ErrorCode.INVALID_FILE_TYPE, errorOf(file("a.jpg", "image/jpeg", HTML)));     // 이름·타입만 이미지, 내용은 HTML
        assertEquals(ErrorCode.INVALID_FILE_TYPE, errorOf(file("a.svg", "image/svg+xml", HTML)));
        verifyNoInteractions(amazonS3);
    }

    @Test
    void 여러_장_중_하나라도_잘못되면_하나도_올리지_않는다() {
        List<MockMultipartFile> files = List.of(file("a.jpg", "image/jpeg", JPEG), file("b.jpg", "image/jpeg", HTML));

        CustomException e = assertThrows(CustomException.class, () -> s3Service.uploadFiles(new ArrayList<>(files)));

        assertEquals(ErrorCode.INVALID_FILE_TYPE, e.getErrorCode());
        verify(amazonS3, never()).putObject(any(String.class), any(String.class), any(java.io.InputStream.class), any());
    }

    @Test
    void 한_번에_6장_이상_올리면_거부() {
        ItemRepository itemRepository = mock(ItemRepository.class);
        S3Service s3 = mock(S3Service.class);
        ItemService itemService = new ItemService(itemRepository, mock(UserRepository.class), mock(ItemImageRepository.class),
                mock(TradeRepository.class), s3, mock(ApplicationEventPublisher.class), mock(BlockRepository.class));
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Item item = Item.builder().id(10L).user(owner).build();
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        List<org.springframework.web.multipart.MultipartFile> six = new ArrayList<>();
        for (int i = 0; i < 6; i++) six.add(file("a" + i + ".jpg", "image/jpeg", JPEG));

        CustomException e = assertThrows(CustomException.class, () -> itemService.uploadImages(10L, six, 1L));
        assertEquals(ErrorCode.TOO_MANY_FILES, e.getErrorCode());
        verifyNoInteractions(s3);
    }
}
