package com.zigu.ziguwas.domains.user.api;

import com.zigu.ziguwas.domains.user.dto.mypage.response.MyPageMainResDto;
import com.zigu.ziguwas.domains.user.dto.mypage.response.MyRegisteredItemResDto;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "MyPage API", description = "마이페이지 관련 API")
@RequestMapping(value = "/api/v1/mypage", produces = MediaType.APPLICATION_JSON_VALUE)
public interface MyPageApi {

    @Operation(
            summary = "마이페이지 메인 정보 조회",
            description = "사용자의 프로필 요약 정보와 최근 등록한 물건 리스트 3개를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyPageMainResDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(examples = {
                            @ExampleObject(name = "인증되지 않은 사용자", value = """
                                { "status": 401, "message": "인증되지 않은 사용자 입니다." }
                                """)
                    })
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "허용되지 않은 접근",
                    content = @Content(examples = {
                            @ExampleObject(name = "허용되지 않은 접근", value = """
                                { "status": 403, "message": "허용되지 않은 접근입니다." }
                                """)
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "데이터를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(name = "사용자 없음", value = """
                        { "status": 404, "message": "사용자를 찾을 수 없습니다." }
                        """))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(examples = @ExampleObject(value = """
                        { "status": 500, "message": "내부 서버 오류입니다." }
                        """))
            )
    })
    @GetMapping
    ResponseEntity<MyPageMainResDto> getMyPageMain(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "내가 등록한 아이템 전체 조회",
            description = "현재 로그인한 사용자가 등록한 모든 아이템 목록을 최신순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MyRegisteredItemResDto.class)))),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(examples = {
                            @ExampleObject(name = "인증되지 않은 사용자", value = """
                                { "status": 401, "message": "인증되지 않은 사용자 입니다." }
                                """)
                    })
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "허용되지 않은 접근",
                    content = @Content(examples = {
                            @ExampleObject(name = "허용되지 않은 접근", value = """
                                { "status": 403, "message": "허용되지 않은 접근입니다." }
                                """)
                    })
            )
    })
    @GetMapping("/items")
    ResponseEntity<List<MyRegisteredItemResDto>> getMyPageItems(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}