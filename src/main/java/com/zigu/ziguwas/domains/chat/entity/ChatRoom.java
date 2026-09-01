package com.zigu.ziguwas.domains.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

//@Entity
@Document(collection="chat_rooms")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatRoom {

    @Id
    private String id;

    private Long itemId;

    // 모든 참여자가 나가 채팅방이 폐쇄된 시각. null이면 아직 참여자가 남아있는 채팅방이다.
    // 이 시각으로부터 보관기간이 지나면 채팅방과 메시지가 통째로 삭제된다.
    // (보관기간 배치가 이 필드만으로 조회하므로 인덱스가 필요하다)
    @Indexed
    private LocalDateTime closedAt;

    /**
     * 마지막 참여자가 나가 채팅방을 폐쇄합니다.
     *
     * 즉시 삭제하지 않고 시각만 기록하는 이유는, 삭제까지 유예 기간을 두어
     * 그동안 대화 내역이 보존되도록 하기 위함이다.
     *
     * @param closedAt 마지막 참여자가 나간 시각
     */
    public void close(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    /**
     * 폐쇄되었던 채팅방을 다시 엽니다.
     *
     * 참여자가 다시 들어오면 삭제 대상에서 제외되어야 하므로 폐쇄 시각을 지운다.
     */
    public void reopen() {
        this.closedAt = null;
    }

    /**
     * 모든 참여자가 나가 폐쇄된 채팅방인지 확인합니다.
     *
     * @return T/F
     */
    public boolean isClosed() {
        return this.closedAt != null;
    }
}
