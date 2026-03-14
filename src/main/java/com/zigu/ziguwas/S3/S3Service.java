package com.zigu.ziguwas.S3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 여러 장의 이미지를 한 번에 S3에 업로드합니다.
     *
     * @param multipartFiles 업로드할 멀티파트 파일 리스트
     * @return S3에 저장된 파일들의 퍼블릭 URL 리스트
     */
    public List<String> uploadFiles(List<MultipartFile> multipartFiles) {
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : multipartFiles) {
            if (file != null && !file.isEmpty()) {
                uploadedUrls.add(uploadSingleFile(file));
            }
        }
        return uploadedUrls;
    }

    /**
     * 단일 파일을 S3 버킷에 업로드하고 접근 URL을 반환합니다.
     */
    private String uploadSingleFile(MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();
        String storeFileName = createStoreFileName(originalFilename);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        try {
            amazonS3.putObject(bucket, storeFileName, multipartFile.getInputStream(), metadata);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FAIL_UPLOAD_FILE);
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
}
