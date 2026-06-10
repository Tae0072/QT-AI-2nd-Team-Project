package com.qtai.domain.member.api;

import java.util.List;

/** 활성 회원 ID 목록 조회 UseCase. */
public interface ListActiveMemberIdsUseCase {

    List<Long> listActiveMemberIds();
}
