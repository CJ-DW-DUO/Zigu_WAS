package com.zigu.ziguwas.domains.university.controller;

import com.zigu.ziguwas.domains.university.service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/universities")
public class UniversityController {

    private final UniversityService universityService;

    /**
     * 모든 대학 정보 목록 조회 API
     *
     * @return 대학 정보 목록
     */
    @GetMapping
    public ResponseEntity<?> getAllUniversities(){
        return ResponseEntity.ok().body(universityService.getAllUniversities());
    }

}
