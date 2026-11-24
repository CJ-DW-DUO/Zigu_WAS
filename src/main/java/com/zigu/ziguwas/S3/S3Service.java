package com.zigu.ziguwas.S3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 업로드를 허용하는 이미지 형식 (Content-Type · 확장자 · 파일 앞부분 시그니처가 모두 맞아야 한다)
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/gif");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "heic", "heif", "gif");

    /**
     * 여러 장의 이미지를 한 번에 S3에 업로드합니다.
     *
     * @param multipartFiles 업로드할 멀티파트 파일 리스트
     * @return S3에 저장된 파일들의 퍼블릭 URL 리스트
     */
    public List<String> uploadFiles(List<MultipartFile> multipartFiles) {
        List<String> uploadedUrls = new ArrayList<>();

        // 한 장이라도 형식이 잘못되었다면 하나도 올리지 않도록 먼저 전부 검사한다.
        for (MultipartFile file : multipartFiles) {
            if (file != null && !file.isEmpty()) {
                validateImage(file);
            }
        }

        for (MultipartFile file : multipartFiles) {
            if (file != null && !file.isEmpty()) {
                uploadedUrls.add(uploadSingleFile(file));
            }
        }
        return uploadedUrls;
    }

    /**
     * 업로드 파일이 허용된 이미지인지 검사합니다. (공개 버킷에 임의 파일이 올라가는 것을 막기 위함)
     * Content-Type 과 확장자는 클라이언트가 마음대로 보낼 수 있으므로 파일 앞부분의 시그니처까지 확인합니다.
     *
     * @throws CustomException 허용되지 않은 형식이면 ErrorCode.INVALID_FILE_TYPE
     */
    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")
                && !ALLOWED_EXTENSIONS.contains(extractExt(originalFilename).toLowerCase())) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAIL);
        }
        if (!hasImageSignature(head)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    /**
     * 파일 앞부분이 JPEG / PNG / GIF / WEBP / HEIC·HEIF 중 하나의 시그니처인지 확인합니다.
     */
    private static boolean hasImageSignature(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return true; // JPEG
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return true; // PNG
        }
        if (h.length >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return true; // GIF
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return true; // WEBP
        }
        // HEIC / HEIF (ISO BMFF): 4~7번째 바이트가 "ftyp"
        return h.length >= 12 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p';
    }

    /**
     * 단일 파일을 S3 버킷에 업로드하고 접근 URL을 반환합니다.
     */
    public String uploadSingleFile(MultipartFile multipartFile) {
        validateImage(multipartFile);

        String originalFilename = multipartFile.getOriginalFilename();
        String storeFileName = createStoreFileName(originalFilename);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        try {
            amazonS3.putObject(bucket, storeFileName, multipartFile.getInputStream(), metadata);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAIL);
        }

        return amazonS3.getUrl(bucket, storeFileName).toString();
    }

    /**
     * S3에 저장될 고유한 파일명을 생성합니다. (UUID 방식)
     */
    private String createStoreFileName(String originalFilename) {
        String ext = extractExt(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + ext;
    }

    /**
     * 원본 파일명에서 확장자를 추출합니다.
     */
    private String extractExt(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "jpeg"; // 확장자가 없는 경우 기본값
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
    }

    /**
     * S3 버킷에서 특정 파일을 삭제합니다.
     * 저장된 전체 URL에서 파일명(Key)만 추출하여 삭제 요청을 보냅니다.
     *
     * @param fileUrl 삭제할 파일의 전체 URL (DB에 저장된 경로)
     * @throws CustomException 파일 삭제 중 오류가 발생할 경우 (ErrorCode.FAIL_DELETE_FILE)
     */
    public void deleteFile(String fileUrl) {
        String fileName = extractFileName(fileUrl);

        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FAIL_DELETE_FILE);
        }
    }

    /**
     * S3 전체 URL에서 객체의 Key(파일명)를 추출합니다.
     * @param fileUrl 파일의 전체 URL
     * @return 추출된 파일명
     */
    private String extractFileName(String fileUrl) {
        // 예: https://bucket-name.s3.region.amazonaws.com/uuid.png -> uuid.png
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }
}
