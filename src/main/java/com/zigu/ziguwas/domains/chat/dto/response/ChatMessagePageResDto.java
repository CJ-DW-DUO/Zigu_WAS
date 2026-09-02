package com.zigu.ziguwas.domains.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatMessagePageResDto {

    // 조회된 메시지 목록
    private List<ChatMessageDetailResDto> content;

    // 마지막 페이지 여부 (true면 다음 페이지를 요청할 필요 없음)
    private boolean last;
}
