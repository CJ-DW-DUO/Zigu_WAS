package com.zigu.ziguwas.domains.report.controller;

import com.zigu.ziguwas.domains.report.dto.ReportReqDto;
import com.zigu.ziguwas.domains.report.service.ReportService;
import com.zigu.ziguwas.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/{itemId}")
    public ResponseEntity<Void> createReport(
            @PathVariable Long itemId,
            @Valid @RequestBody ReportReqDto requestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long reporterId = customUserDetails.getUserId();
        reportService.registerReport(reporterId, itemId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
