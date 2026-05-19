package com.zigu.ziguwas.domains.report.dto;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.report.entity.Report;
import com.zigu.ziguwas.domains.report.entity.ReportCategory;
import com.zigu.ziguwas.domains.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "물건 게시글 신고 요청 정보")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportReqDto {

    @NotEmpty(message = "신고 카테고리를 최소 1개 이상 선택해 주세요.")
    @Schema(description = "신고 카테고리 목록", example = "[\"IRRELEVANT_POST\", \"FRAUD\"]")
    private List<ReportCategory> categories;

    @Schema(description = "기타 상세 사유", example = "이 사람 다른 글에서도 사기 치고 다닙니다.")
    private String reason;

    public Report toEntity(Item item, User reporter) {
        return Report.builder()
                .item(item)
                .user(reporter)
                .repCategory(this.categories)
                .reason(this.reason)
                .build();
    }
}