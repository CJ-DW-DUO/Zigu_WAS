package com.zigu.ziguwas.domains.university.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UniversitiesResDto {

    @Schema(description = "대학교ID", example = "1")
    private Long univId;

    @Schema(description = "학교명", example = "지구대학교")
    private String univName;

    @Schema(description = "학교이메일", example = "zigu.edu.kr")
    private String univEmail;
}
