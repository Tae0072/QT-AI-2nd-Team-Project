package com.qtai.domain.member.internal;

import org.springframework.data.jpa.repository.JpaRepository;

/** 닉네임 변경 이력 기록(원본 service-user). 닉네임 변경 시 append-only로 저장한다. */
public interface NicknameChangeHistoryRepository extends JpaRepository<NicknameChangeHistory, Long> {
}
