package com.qtai.domain.admin.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 계정 엔티티.
 *
 * <p>ERD: admin_users 테이블.
 * <p>members.role=ADMIN인 회원과 1:1 매핑되며, 세부 관리자 역할(admin_role)을 관리한다.
 *
 * <p>CLAUDE.md §5: 관리자 API 진입 시 Spring Security가 ROLE_ADMIN을 1차 검증하고,
 * 서비스 레이어에서 이 엔티티의 admin_role을 2차 검증한다.
 *
 * <p>ERD §2.31 역할 정책:
 * <ul>
 *   <li>CONTENT_CREATOR: 검증용 자료 제작, 내부 콘텐츠 제작 권한</li>
 *   <li>일반 OPERATOR: 검증용 한국어 주석 원문에 직접 접근 불가</li>
 *   <li>배치/검증 agent: admin_users가 아닌 service_accounts로 관리</li>
 * </ul>
 */
@Entity
@Table(name = "admin_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUser extends BaseEntity {

    /** 연결 회원 ID (members.id). */
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    /** 관리자 세부 역할. */
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_role", nullable = false, length = 30)
    private AdminRole adminRole;

    /** 관리자 계정 상태. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminStatus status;

    /** 관리자 웹 로그인 아이디(UNIQUE). 카카오 대체 자체 로그인용. 기존 행은 null일 수 있다. */
    @Column(name = "username", length = 100, unique = true)
    private String username;

    /** 비밀번호 BCrypt 해시. 평문 저장 금지(CLAUDE.md §8/§9). 기존 행은 null일 수 있다. */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Builder
    public AdminUser(Long memberId, AdminRole adminRole) {
        this.memberId = memberId;
        this.adminRole = adminRole;
        this.status = AdminStatus.ACTIVE;
    }

    /** 로그인 자격(아이디·비밀번호 해시) 설정/변경. 비밀번호는 호출 전 BCrypt 해시여야 한다. */
    public void assignCredentials(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /** 관리자 계정 활성 여부. */
    public boolean isActive() {
        return this.status == AdminStatus.ACTIVE;
    }

    /** 특정 역할을 보유하고 있는지 확인. SUPER_ADMIN은 모든 역할을 포함한다. */
    public boolean hasRole(AdminRole requiredRole) {
        if (this.adminRole == AdminRole.SUPER_ADMIN) {
            return true;
        }
        return this.adminRole == requiredRole;
    }

    /** 관리자 역할 변경. */
    public void changeRole(AdminRole newRole) {
        this.adminRole = newRole;
    }

    /** 관리자 계정 비활성화. */
    public void disable() {
        this.status = AdminStatus.DISABLED;
    }

    /** 관리자 계정 활성화. */
    public void enable() {
        this.status = AdminStatus.ACTIVE;
    }
}
