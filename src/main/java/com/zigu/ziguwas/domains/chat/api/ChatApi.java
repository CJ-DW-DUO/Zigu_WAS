package com.zigu.ziguwas.domains.chat.api;

import com.zigu.ziguwas.domains.chat.dto.request.CreateChatRoomReqDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatImageUploadResDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatMessageDetailResDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomItemAndTradeInfoResDto;
import com.zigu.ziguwas.domains.chat.dto.response.ChatRoomPreviewResDto;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Chat API", description = "채팅 관련 API 입니다.")
public interface ChatApi {

    @Operation(summary = "채팅방 목록 조회", description = "사용자가 참여 중인 모든 채팅방의 미리보기 목록을 조회합니다. 나간 채팅방은 목록에 포함되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatRoomPreviewResDto.class)))
            )
    })
    ResponseEntity<?> getChatroomsPreview(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 상세 대화 내역 및 정보를 조회합니다. 이전에 채팅방을 나간 적이 있다면 나간 시각 이후의 메시지만 조회됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatMessageDetailResDto.class)))
            ),
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
            @ApiResponse(responseCode = "400", description = "채팅방 생성 실패"),
            @ApiResponse(responseCode = "403", description = "다른 대학교 매물에 대한 채팅 생성 시도",
                    content = @Content(examples = @ExampleObject(value = """
                            { "status": 403, "message": "해당 대학에 대한 권한이 없습니다." }
                            """))
            )
    })
    ResponseEntity<?> createChatroom(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CreateChatRoomReqDto dto
    );

    @Operation(summary = "채팅 이미지 업로드", description = "이미지를 S3에 업로드하고 URL을 반환합니다. 반환된 URL은 실제 메시지 전송 시 STOMP SEND 페이로드의 imageUrl로 담아 보내야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 성공",
                    content = @Content(schema = @Schema(implementation = ChatImageUploadResDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "파일 업로드 실패 (파일 없음/S3 업로드 오류)"),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    ResponseEntity<?> uploadChatImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "채팅방 ID", required = true) @PathVariable String chatRoomId,
            @Parameter(description = "업로드할 이미지 파일", required = true) MultipartFile image
    );

    @Operation(summary = "채팅방 나가기", description = """
            요청한 사용자의 채팅방 목록에서 해당 채팅방을 숨깁니다.

            - 채팅방이 즉시 삭제되지는 않으며, 상대방에게는 나갔다는 사실이 표시되지 않습니다.
            - 나간 뒤에는 채팅방 목록 조회/상세 조회/메시지 전송/구독이 모두 차단됩니다.
            - 상대방이 새 메시지를 보내면 채팅방이 다시 목록에 나타나며, 이때 나가기 이전의 대화 내역은 보이지 않고 새로 받은 메시지부터 보입니다.
            - 두 참여자가 모두 나간 경우에는 채팅방과 메시지가 실제로 삭제됩니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "나가기 성공 (응답 본문 없음)"),
            @ApiResponse(responseCode = "400", description = "이미 나간 채팅방",
                    content = @Content(examples = @ExampleObject(value = """
                            { "status": 400, "message": "이미 나간 채팅방입니다." }
                            """))
            ),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(examples = @ExampleObject(value = """
                            { "status": 403, "message": "허용되지 않은 접근입니다." }
                            """))
            ),
            @ApiResponse(responseCode = "404", description = "채팅방 또는 사용자 정보를 찾을 수 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "사용자 없음", value = """
                                    { "status": 404, "message": "사용자를 찾을 수 없습니다." }
                                    """),
                            @ExampleObject(name = "채팅방 없음", value = """
                                    { "status": 404, "message": "해당 채팅방은 존재하지 않습니다." }
                                    """)
                    })
            )
    })
    ResponseEntity<?> leaveChatroom(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "나갈 채팅방 ID", required = true) @PathVariable String chatRoomId
    );
}
