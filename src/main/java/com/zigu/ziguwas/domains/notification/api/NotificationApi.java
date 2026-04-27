package com.zigu.ziguwas.domains.notification.api;

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
                                                  "readAt": null
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
            @Parameter(description = "읽음 처리할 알림 ID") @PathVariable Long notificationId
    );
}

