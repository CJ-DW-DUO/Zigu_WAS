package com.zigu.ziguwas.domains.university.repository;

import com.zigu.ziguwas.domains.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {

    /**
     * 해당 도메인을 가진 대학이 존재하는지
     *
     * @param univEmail 대학 이메일
     * @return T/F
     */
    boolean existsByUnivEmail(String univEmail);

    /**
     * 해당 도메인을 가진 대학 정보 가져오기
     *
     * @param univEmail 대학 이메일
     * @return 대학 객체
     */
    Optional<University> findByUnivEmail(String univEmail);
}
