package com.zigu.ziguwas.domains.item.api;

import com.zigu.ziguwas.domains.item.dto.reqdto.ItemRegisterReqDto;
import com.zigu.ziguwas.domains.item.dto.reqdto.ItemUpdateReqDto;
import com.zigu.ziguwas.domains.item.dto.resdto.ItemResDto;
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
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Item API", description = "아이템 등록 및 이미지 업로드 API")
@RequestMapping(value = "/api/v1/items", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ItemApi {

    @Operation(
            summary = "1단계: 아이템 기본 정보 등록",
            description = "새로운 아이템의 텍스트 정보를 먼저 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "아이템 등록 성공",
                    content = @Content(schema = @Schema(implementation = ItemResDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 인증 실패",
                    content = @Content(examples = {
                            @ExampleObject(name = "인증 실패", value = """
                                { "status": 400, "message": "인증되지 않은 사용자 입니다." }
                                """),
                            @ExampleObject(name = "접근 권한 없음", value = """
                                { "status": 400, "message": "허용되지 않은 접근입니다." }
                                """)
                    })
            ),
            @ApiResponse(responseCode = "404", description = "유저 정보 없음",
                    content = @Content(examples = @ExampleObject("""
                            { "status": 404, "message": "유저를 찾을 수 없습니다." }
                            """))
            )
    })
    @PostMapping
    ResponseEntity<ItemResDto> registerItem(
            @RequestBody @Valid ItemRegisterReqDto itemRegisterReqDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "2단계: 아이템 다중 이미지 업로드",
            description = "등록된 아이템에 실제 이미지 파일들을 업로드합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공",
                    content = @Content(schema = @Schema(implementation = ItemResDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "업로드 실패 또는 권한 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "파일 업로드 실패", value = """
                                { "status": 400, "message": "파일업로드 실패" }
                                """),
                            @ExampleObject(name = "허용되지 않은 접근", value = """
                                { "status": 400, "message": "허용되지 않은 접근입니다." }
                                """)
                    })
            ),
            @ApiResponse(responseCode = "404", description = "아이템 없음",
                    content = @Content(examples = @ExampleObject("""
                            { "status": 404, "message": "아이템을 찾을 수 없습니다." }
                            """))
            )
    })
    @PostMapping(value = "/{itemId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ItemResDto> uploadItemImages(
            @Parameter(description = "이미지를 추가할 아이템 ID", required = true)
            @PathVariable("itemId") Long itemId,

            @Parameter(
                    description = "업로드할 이미지 파일 리스트",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary")))
            )
            @RequestPart("images") List<MultipartFile> images,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "아이템 이미지 삭제",
            description = "아이템에 등록된 특정 이미지를 삭제합니다. 본인의 아이템인 경우에만 삭제가 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이미지 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 권한 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "허용되지 않은 접근", value = """
                                { "status": 400, "message": "허용되지 않은 접근입니다." }
                                """),
                            @ExampleObject(name = "아이템-이미지 불일치", value = """
                                { "status": 400, "message": "해당 item에 속하지않은 image 입니다." }
                                """)
                    })
            ),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "아이템 없음", value = """
                                { "status": 404, "message": "아이템을 찾을 수 없습니다." }
                                """),
                            @ExampleObject(name = "이미지 없음", value = """
                                { "status": 404, "message": "이미지를 찾을 수 없습니다." }
                                """)
                    })
            )
    })
    @DeleteMapping("/{itemId}/images/{imageId}")
    ResponseEntity<String> deleteItemImages(
            @PathVariable("itemId") Long itemId,
            @PathVariable("imageId") Long imageId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "아이템 수정", description = "기존에 등록된 아이템의 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "404", description = "아이템을 찾을 수 없음")
    })
    @PutMapping("/{itemId}")
    ResponseEntity<ItemResDto> updateItem(
            @PathVariable("itemId") Long itemId,
            @RequestBody @Valid ItemUpdateReqDto itemUpdateReqDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}