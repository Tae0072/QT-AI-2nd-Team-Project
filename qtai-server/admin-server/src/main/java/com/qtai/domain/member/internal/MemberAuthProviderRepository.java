package com.qtai.domain.member.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * OAuth 제공자 연동 정보 영속성 포트.
 */
public interface MemberAuthProviderRepository extends JpaRepository<MemberAuthProvider, Long> {

    Optional<MemberAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);
}
