package com.zigu.ziguwas.domains.item.listener;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.item.event.ItemImagesDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 아이템/이미지 삭제가 DB에 커밋된 이후에만 실제 S3 파일 삭제를 수행합니다.
 *
 * DB 트랜잭션 안에서 S3(외부 API) 호출을 동기로 묶어두면, S3 응답이 느려질 때
 * DB 커넥션과 락을 계속 붙잡고 있어 다른 요청까지 지연되는 문제(삭제 시 무한로딩)가
 * 발생할 수 있어, 커밋 이후로 분리했습니다. 파일별로 실패를 격리해 일부 삭제
 * 실패가 나머지 파일 정리를 막지 않도록 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemImageCleanupListener {

    private final S3Service s3Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleItemImagesDeleted(ItemImagesDeletedEvent event) {
        for (String imageUrl : event.imageUrls()) {
            try {
                s3Service.deleteFile(imageUrl);
            } catch (Exception e) {
                log.warn("S3 이미지 삭제 실패 - imageUrl: {}", imageUrl, e);
            }
        }
    }
}
