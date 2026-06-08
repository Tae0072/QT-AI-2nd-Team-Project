package com.qtai.domain.admin.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * dev 프로파일 전용 관리자 계정 시드.
 *
 * <p>dev-bypass 환경에서 관리자 웹(admin-web)이 실제 관리자 API를 호출하려면
 * {@code admin_users}에 활성 관리자 행이 있어야 한다(AdminService 2차 검증).
 * 이 러너는 지정한 회원 id({@code qtai.dev.admin-member-id}, 기본 1)를
 * {@code SUPER_ADMIN} 활성 관리자로 멱등 시드한다.
 *
 * <p>전제: 해당 회원이 먼저 존재해야 한다. 회원 시드(member 도메인)가 {@code @Order(10)}로
 * 먼저 실행되도록 보장하며(admin_users.member_id FK 선행), 회원이 아직 없으면
 * FK 위반을 경고 로그로 흡수하고 다음 기동에서 재시도한다.
 *
 * <p>운영 사고 방지 가드(다른 dev 컴포넌트와 동일):
 * <ul>
 *   <li>{@code @Profile("dev")} — prod·default 프로파일에서는 빈 등록 자체가 안 됨</li>
 *   <li>{@code @ConditionalOnProperty(dev-bypass)} — 토글 ON일 때만 활성</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "qtai.security.dev-bypass", havingValue = "true")
@Order(20) // DevMemberSeedRunner(@Order(10)) 이후 실행 — 회원 존재 후 admin_users 시드 (FK 순서)
@RequiredArgsConstructor
class DevAdminSeedRunner implements ApplicationRunner {

    private static final AdminRole DEV_ADMIN_ROLE = AdminRole.SUPER_ADMIN;

    private final AdminUserRepository adminUserRepository;

    /** dev 관리자로 시드할 회원 id. 기본값은 dev 회원 시드의 관례 id(1). admin-web의 X-Dev-User-Id와 일치해야 한다. */
    @Value("${qtai.dev.admin-member-id:1}")
    private long adminMemberId;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.findByMemberId(adminMemberId).isPresent()) {
            return;
        }
        try {
            adminUserRepository.save(AdminUser.builder()
                    .memberId(adminMemberId)
                    .adminRole(DEV_ADMIN_ROLE)
                    .build());
            log.warn("⚠️ DEV 관리자 시드 — memberId={}를 {} 활성 관리자로 등록했습니다. dev 전용입니다.",
                    adminMemberId, DEV_ADMIN_ROLE);
        } catch (DataIntegrityViolationException e) {
            // 회원 미존재(FK) 또는 동시 시드 경합. 회원 시드 선행 여부와 id 설정을 확인.
            log.warn("DEV 관리자 시드 보류 — memberId={} 회원 미존재 또는 경합. " +
                    "DevMemberSeedRunner 선행 여부와 qtai.dev.admin-member-id 설정을 확인하세요.", adminMemberId);
        }
    }
}
