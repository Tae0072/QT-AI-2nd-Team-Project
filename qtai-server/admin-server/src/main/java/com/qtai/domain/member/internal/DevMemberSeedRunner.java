package com.qtai.domain.member.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "qtai.security.dev-bypass", havingValue = "true")
@Order(10) // DevAdminSeedRunner(@Order(20))보다 먼저 실행해 dev 회원을 시드한다 (admin_users.member_id FK 선행)
@RequiredArgsConstructor
class DevMemberSeedRunner implements ApplicationRunner {

    private static final long DEV_KAKAO_ID = 9_000_000_001L;
    private static final String DEV_NICKNAME = "dev-user";

    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByKakaoId(DEV_KAKAO_ID)) {
            return;
        }
        try {
            memberRepository.saveAndFlush(Member.builder()
                    .kakaoId(DEV_KAKAO_ID)
                    .nickname(DEV_NICKNAME)
                    .email("dev-user@example.test")
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // Another dev-profile process seeded the reserved member first.
        }
    }
}
