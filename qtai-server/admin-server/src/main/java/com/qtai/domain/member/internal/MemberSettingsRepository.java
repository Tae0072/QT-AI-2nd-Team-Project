package com.qtai.domain.member.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberSettingsRepository extends JpaRepository<MemberSettings, Long> {

    Optional<MemberSettings> findByMemberId(Long memberId);
}
