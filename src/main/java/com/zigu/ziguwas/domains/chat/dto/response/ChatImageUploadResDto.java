package com.zigu.ziguwas.domains.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatImageUploadResDto {

    // S3에 업로드된 이미지 URL (이후 STOMP 메시지 전송 시 imageUrl로 사용)
    private String imageUrl;

}
