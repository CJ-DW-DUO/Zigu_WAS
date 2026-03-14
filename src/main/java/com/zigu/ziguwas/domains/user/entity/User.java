package com.zigu.ziguwas.domains.user.entity;

import com.zigu.ziguwas.domains.university.entity.University;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * @author 곽동욱
 * @since 2026.03.12
 *
 * User Entity
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class User {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK - 대학ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "univ_id", nullable = false)
    private University univId;

    // 이메일
    @Column(nullable = false, unique = true)
    private String email;

    // 닉네임
    @Column(unique = true , nullable = false)
    private String nickname;

    // 비밀번호
    @Column(nullable = false)
    private String password;

    // 프로필 사진 주소
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    // 인증 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "veri_status")
    private VerificationStatus veriStatus;

}
