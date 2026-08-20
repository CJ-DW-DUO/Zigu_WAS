package com.zigu.ziguwas.domains.item.event;

import java.util.List;

/**
 * 아이템/이미지 삭제가 DB에 확정된 뒤, 실제 S3 원본 파일 정리를 요청하는 도메인 이벤트입니다.
 *
 * S3 삭제(외부 API 호출)를 DB 트랜잭션 밖에서 처리하기 위해 사용합니다.
 * 서비스 계층은 이 이벤트만 발행하고, 실제 S3 삭제는 리스너가 담당합니다.
 */
public record ItemImagesDeletedEvent(
        List<String> imageUrls
) {
}
