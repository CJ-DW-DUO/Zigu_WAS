**1. 웹소켓 소켓 연결 (Connect)**

- **엔드포인트 URL:** `wss://[서버도메인]/ws-stomp`

- **Header:** `Authorization: Bearer {JWT토큰}`

- _설명:_ 소켓을 처음 연결하는 STOMP CONNECT 프레임 헤더에 반드시 JWT 토큰을 담아야합니다.


**2. 메시지 수신 (Subscribe - 채팅방 입장 시, 구독)**

- **구독(Sub) URL:** `/sub/chat/room/{chatRoomId}`

- **수신받는 데이터(Response Body):**

  JSON

    ```
    {
      "message": "안녕"
    }
    ```

  _(※ 현재 백엔드 설정상 전송된 메시지 텍스트를 그대로 브로드캐스팅합니다.)_

- 이후 뒤로가기를 눌러 채팅방에서 잠시 나올경우(퇴장X) 구독을 잠시 취소하는 방법 등을 사용하여 처리해야합니다(프론트엔드에서 어떻게 처리되는지 모르겠네요)

**3. 메시지 발신 (Publish - 메시지 보낼 때)**

- **발신(Pub) URL:** `/pub/chat/v1/chatrooms/{chatRoomId}`

- **보내는 데이터(Request Body):**

  JSON

    ```
    {
      "message": "보낼 메시지 내용"
    }
    ```


**4. 1:1 채팅방 생성 (REST API - 최초 채팅 시작 시)**

- **Method / URL:** `POST /api/v1/chatrooms`

- **보내는 데이터(Request Body):**

  JSON

    ```
    {
      "receiverId": 2,
      "itemId": 1
    }
    ```

  _설명:_ `receiverId`는 상대방의 PK, `itemId`는 거래할 물품의 PK입니다.