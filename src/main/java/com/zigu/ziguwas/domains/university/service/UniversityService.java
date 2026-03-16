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
     * 엔티티에서 가져온 대학 리스트를 응답 DTO로 반환
     *
     * @param universities 대학 엔티티
     * @return 대학 정보 응답 DTO
     */
    private List<UniversitiesResDto> fromEntity(List<University> universities){
        // 1. 대학교 응답 DTO 리스트 할당
        List<UniversitiesResDto> dtos = new ArrayList<>();

        // 2. 대학교 응답 매핑
        for(University univ :  universities) {
            dtos.add(UniversitiesResDto.builder()
                    .univId(univ.getUnivId())
                    .univName(univ.getUnivName())
                    .univEmail(univ.getUnivEmail())
                    .build());
        }

        return dtos;
    }

    /**
     * 모든 대학 정보 목록 조회 서비스
     *
     * @return 모든 대학 정보 목록 DTO
     */
    public List<UniversitiesResDto> getAllUniversities() {

        // 1. 모든 등록된 대학교 가져오기
        List<University> universities = universityRepository.findAll();

        // 2. 대학교 응답 DTO 매핑 후 반환
        return fromEntity(universities);
    }

    /**
     * 대학 검색 서비스
     *
     * @param name 검색어
     * @return 검색된 대학들
     */
    public List<UniversitiesResDto> getUniversities(String name) {

        // 1. 검색어를 기반으로 대학 리스트 가져오기
        List<University> universities = universityRepository.findByUnivNameContains(name);

        // 2. 대학교 응답 DTO 매핑 후 반환
        return fromEntity(universities);
    }
}
