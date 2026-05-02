# Zigu WAS Chat WebSocket Test

이 페이지는 `WebSocketConfig` 기준으로 STOMP 연결을 테스트하기 위한 간단한 웹 페이지입니다.

## 전제 조건
- 서버가 실행 중이어야 합니다.
- 유효한 JWT Access Token이 필요합니다.
- 채팅방이 이미 존재해야 합니다.

## 실행 방법
1. 브라우저에서 `tools/ws-test/index.html`을 엽니다.
2. 아래 값을 입력합니다.
   - WebSocket URL: 기본 `ws://localhost:8080/ws-stomp`
   - ChatRoom ID: 테스트할 채팅방 ID
   - JWT Access Token: `Bearer` 접두사 없이 토큰만
3. `Connect` 클릭 후, 메시지를 입력하고 `Send`를 누릅니다.

## 메시지 경로
- 송신: `/pub/chat/v1/chatrooms/{chatRoomId}`
- 수신 구독: `/sub/chat/room/{chatRoomId}`

## 데이터 준비 힌트
- **유저 2명 이상**이 있어야 서로 채팅 테스트가 가능합니다.
- **아이템**이 존재해야 채팅방 생성이 가능합니다.
- REST로 채팅방 생성 후 `chatRoomId`를 확인하세요.

예시 (REST 생성):
```
POST /api/v1/chatrooms
{
  "itemId": 1,
  "receiverId": 2
}
```

토큰 발급 방식은 프로젝트의 로그인 API에 맞춰 준비해주세요.

