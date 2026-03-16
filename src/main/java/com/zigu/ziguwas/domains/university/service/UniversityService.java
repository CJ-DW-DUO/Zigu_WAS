package com.zigu.ziguwas.domains.university.service;

import com.zigu.ziguwas.domains.university.controller.dto.response.UniversitiesResDto;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository universityRepository;

    /**
     * 모든 대학 정보 목록 조회 서비스
     *
     * @return 모든 대학 정보 목록 DTO
     */
    public List<UniversitiesResDto> getAllUniversities() {

        // 1. 모든 등록된 대학교 가져오기
        List<University> universities = universityRepository.findAll();

        // 2. 대학교 응답 DTO 리스트 할당
        List<UniversitiesResDto> dtos = new ArrayList<>();

        // 3. 대학교 응답
        for(University univ :  universities) {
            dtos.add(UniversitiesResDto.builder()
                    .univId(univ.getUnivId())
                    .univName(univ.getUnivName())
                    .univEmail(univ.getUnivEmail())
                    .build());
        }

        // 4. 응답 DTO 리스트 반환
        return dtos;
    }
}
