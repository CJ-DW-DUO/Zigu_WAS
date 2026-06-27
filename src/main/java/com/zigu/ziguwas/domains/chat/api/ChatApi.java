package com.zigu.ziguwas.domains.chat.api;

import com.zigu.ziguwas.domains.chat.dto.request.CreateChatRoomReqDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomItemAndTradeInfoResDto;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Chat API", description = "채팅 관련 API 입니다.")
public interface ChatApi {

    @Operation(summary = "채팅방 목록 조회", description = "사용자가 참여 중인 모든 채팅방의 미리보기 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<?> getChatroomsPreview(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 상세 대화 내역 및 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    ResponseEntity<?> getChatroomDetail(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable String chatRoomId,
            @RequestParam Integer page,
            @RequestParam Integer size
    );

    @Operation(summary = "채팅방 거래/물품 정보 조회", description = "특정 채팅방과 연결된 물품 정보 및 거래 상태를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatRoomItemAndTradeInfoResDto.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "chatroomId": 1,
                                      "itemId": 11,
                                      "itemTitle": "캐논 카메라 렌즈",
                                      "itemPrice": 15000,
                                      "imageUrl": "https://example.com/item.jpg",
                                      "userRole": "RENTEE",
                                      "tradeStatus": "REQUESTED"
                                    }
                                    """))
            ),
            @ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            { "status": 403, "message": "허용되지 않은 접근입니다." }
                            """))
            ),
            @ApiResponse(responseCode = "404", description = "채팅방, 물품 또는 사용자 정보를 찾을 수 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "사용자 없음", value = """
                                    { "status": 404, "message": "사용자를 찾을 수 없습니다." }
                                    """),
                            @ExampleObject(name = "채팅방 없음", value = """
                                    { "status": 404, "message": "해당 채팅방은 존재하지 않습니다." }
                                    """),
                            @ExampleObject(name = "물품 없음", value = """
                                    { "status": 404, "message": "아이템을 찾을 수 없습니다." }
                                    """)
                    })
            )
    })
    ResponseEntity<?> getChatroomItemInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "조회할 채팅방 ID", required = true, example = "1") @PathVariable String chatRoomId
    );

    @Operation(summary = "1대1 채팅방 생성", description = "특정 상대방과의 1대1 채팅방을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "400", description = "채팅방 생성 실패")
    })
    ResponseEntity<?> createChatroom(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CreateChatRoomReqDto dto
    );
}
