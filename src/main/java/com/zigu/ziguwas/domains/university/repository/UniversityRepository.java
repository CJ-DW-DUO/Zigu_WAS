package com.zigu.ziguwas.domains.university.repository;

import com.zigu.ziguwas.domains.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {


    boolean existsByUnivEmail(String domain);
}
