package com.zigu.ziguwas.domains.report.service;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.report.dto.ReportReqDto;
import com.zigu.ziguwas.domains.report.entity.Report;
import com.zigu.ziguwas.domains.report.repository.ReportRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    /**
     * 사용자의 신고 요청을 검증하고 데이터베이스에 신고 내역을 저장합니다.
     *
     * @param reporterId 신고를 요청하는 사용자의 식별자
     * @param itemId 신고 대상이 되는 물건 게시글의 식별자
     * @param requestDto 신고 카테고리 및 상세 사유가 포함된 요청 데이터 객체
     * @throws CustomException 사용자 혹은 아이템이 존재하지 않거나 자가 신고 및 중복 신고를 시도할 경우 발생
     */
    @Transactional
    public void registerReport(Long reporterId, Long itemId, ReportReqDto requestDto) {

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        if (item.getUser().getId().equals(reporterId)) {
            throw new CustomException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        boolean isAlreadyReported = reportRepository.existsByUserAndItem(reporter, item);
        if (isAlreadyReported) {
            throw new CustomException(ErrorCode.ALREADY_REPORTED_ITEM);
        }

        Report report = requestDto.toEntity(item, reporter);

        reportRepository.save(report);
    }
}
