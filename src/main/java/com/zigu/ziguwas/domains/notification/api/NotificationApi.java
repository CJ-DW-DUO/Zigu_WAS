package com.zigu.ziguwas.domains.notification.api;

import com.zigu.ziguwas.domains.notification.dto.request.NotificationSettingReqDto;
import com.zigu.ziguwas.domains.notification.dto.request.PushTokenDeleteReqDto;
import com.zigu.ziguwas.domains.notification.dto.request.PushTokenRegisterReqDto;
import com.zigu.ziguwas.domains.notification.dto.response.NotificationListResDto;
import com.zigu.ziguwas.domains.notification.dto.response.NotificationSettingResDto;
import com.zigu.ziguwas.domains.notification.dto.response.NotificationUnreadCountResDto;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Notification API", description = "알림 관련 API 입니다.")
public interface NotificationApi {

    @Operation(summary = "내 알림 목록 조회", description = "로그인 사용자의 알림 목록을 최신순으로 페이지 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class),
                            examples = {
                                    @ExampleObject(name = "조회 성공", value = """
                                            {
                                              "content": [
                                                {
                                                  "notificationId": 12,
                                                  "type": "RENTAL_REQUEST",
                                                  "title": "새로운 대여 요청",
                                                  "content": "홍길동님이 카메라 대여를 요청했어요.",
                                                  "receivedAt": "2026-04-27T14:30:00",
                                                  "isRead": false,
                                                  "readAt": null,
                                                  "referenceId": "42"
                                                }
                                              ],
                                              "pageable": {
                                                "pageNumber": 0,
                                                "pageSize": 10,
                                                "sort": {
                                                  "sorted": true,
                                                  "unsorted": false,
                                                  "empty": false
                                                },
                                                "offset": 0,
                                                "paged": true,
                                                "unpaged": false
                                              },
                                              "last": true,
                                              "totalElements": 1,
                                              "totalPages": 1,
                                              "first": true,
                                              "size": 10,
                                              "number": 0,
                                              "sort": {
                                                "sorted": true,
                                                "unsorted": false,
                                                "empty": false
                                              },
                                              "numberOfElements": 1,
                                              "empty": false
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> getMyNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "페이지 정보(page, size, sort)") Pageable pageable
    );

    @Operation(summary = "알림 단건 조회", description = "특정 알림 하나를 조회합니다. 목록 조회(getMyNotifications)와 동일한 응답 형태이며, 본인 소유 알림만 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificationListResDto.class),
                            examples = {
                                    @ExampleObject(name = "조회 성공", value = """
                                            {
                                              "notificationId": "12",
                                              "type": "RENTAL_REQUEST",
                                              "title": "새로운 대여 요청",
                                              "content": "홍길동님이 카메라 대여를 요청했어요.",
                                              "receivedAt": "2026-04-27T14:30:00",
                                              "isRead": false,
                                              "readAt": null,
                                              "referenceId": "42"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "알림이 존재하지 않거나 본인 소유가 아님",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"해당 알림을 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> getNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "조회할 알림 ID") @PathVariable String notificationId
    );

    @Operation(summary = "미읽음 알림 개수 조회", description = "로그인 사용자의 미읽음 알림 개수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificationUnreadCountResDto.class),
                            examples = {
                                    @ExampleObject(name = "조회 성공", value = """
                                            {
                                              "unreadCount": 3
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> getUnreadCount(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "400", description = "알림 수신자와 로그인 정보 불일치",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 400, \"message\": \"알림 수신자와 로그인 정보가 일치하지 않습니다.\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "알림 또는 사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "알림 없음", value = "{\"status\": 404, \"message\": \"해당 알림을 찾을 수 없습니다.\"}"),
                            @ExampleObject(name = "사용자 없음", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> markAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "읽음 처리할 알림 ID") @PathVariable String notificationId
    );

    @Operation(summary = "채팅방 알림 전체 읽음 처리", description = "특정 채팅방에 대한 로그인 사용자의 채팅 알림을 모두 읽음 상태로 변경합니다. 이미 읽었거나 미읽음 알림이 없어도 성공하며, 같은 요청을 여러 번 보내도 안전합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 참여자가 아님",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 403, \"message\": \"허용되지 않은 접근입니다.\"}")
                    }))
    })
    ResponseEntity<?> markChatRoomNotificationsAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "읽음 처리할 채팅 알림이 속한 채팅방 ID") @PathVariable String chatRoomId
    );

    @Operation(summary = "전체 알림 읽음 처리", description = "로그인 사용자의 모든 미읽음 알림을 읽음 상태로 변경합니다. 이미 전부 읽었거나 미읽음 알림이 없어도 성공하며, 같은 요청을 여러 번 보내도 안전합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공")
    })
    ResponseEntity<?> markAllAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "알림 수신 설정 조회", description = "로그인 사용자의 알림 수신 설정(채팅/거래/마케팅)을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificationSettingResDto.class),
                            examples = {
                                    @ExampleObject(name = "조회 성공", value = """
                                            {
                                              "userId": 1,
                                              "chatNotiEnabled": true,
                                              "tradeNotiEnabled": true,
                                              "marketingNotiEnabled": false
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> getSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "알림 수신 설정 변경", description = "로그인 사용자의 알림 수신 설정(채팅/거래/마케팅)을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificationSettingResDto.class),
                            examples = {
                                    @ExampleObject(name = "변경 성공", value = """
                                            {
                                              "userId": 1,
                                              "chatNotiEnabled": false,
                                              "tradeNotiEnabled": true,
                                              "marketingNotiEnabled": true
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> updateSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody NotificationSettingReqDto dto
    );

    @Operation(summary = "푸시 토큰 등록", description = "로그인 사용자의 기기 푸시 토큰을 등록합니다. 동일 토큰이 이미 등록되어 있으면 새로 만들지 않고 소유자/플랫폼만 갱신합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 400, \"message\": \"토큰은 필수 입력입니다.\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> registerPushToken(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody PushTokenRegisterReqDto dto
    );

    @Operation(summary = "푸시 토큰 삭제", description = "로그아웃하거나 토큰이 더 이상 유효하지 않을 때 본인 소유 푸시 토큰을 삭제합니다. 이미 없는 토큰이어도 에러 없이 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공(토큰이 없었어도 200)"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 400, \"message\": \"토큰은 필수 입력입니다.\"}")
                    }))
    })
    ResponseEntity<?> deletePushToken(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody PushTokenDeleteReqDto dto
    );
}

