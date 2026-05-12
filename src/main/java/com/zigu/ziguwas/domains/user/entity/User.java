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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@SQLDelete(sql = "UPDATE user SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class User {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK - 대학ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "univ_id", nullable = false)
    private University univ;

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

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false; // 기본 false

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "del_reson")
    private String delReson;

    public void updateNickname(String nickname){
        this.nickname = nickname;
    }

    public void updateProfileImage(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public void updatePassWord(String encodePassword) {
        this.password = encodePassword;
    }

    /**
     * 회원 탈퇴 시 개인정보를 식별할 수 없도록 마스킹하고 랜덤값으로 대체한다.
     * 이메일과 닉네임 중복 제약 조건을 회피하기 위해 UUID를 활용한다.
     */
    public void maskPersonalInfo() {
        this.email = "deleted_" + UUID.randomUUID().toString().substring(0, 8);
        this.nickname = "탈퇴사용자_" + UUID.randomUUID().toString().substring(0, 8);
        this.profilePhotoUrl = null;
        this.password = "DELETED_USER_" + UUID.randomUUID().toString().substring(0, 4);
    }

    /**
     * 회원 탈퇴 시 필요한 정보 변경을 처리합니다.
     * @param reason 탈퇴 사유
     */
    public void applyWithdrawal(String reason) {
        this.delReson = reason;
        this.maskPersonalInfo();
    }
}
