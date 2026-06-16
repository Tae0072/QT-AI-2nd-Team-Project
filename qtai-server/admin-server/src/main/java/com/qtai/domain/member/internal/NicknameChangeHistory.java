package com.qtai.domain.member.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 닉네임 변경 이력 (F-04/F-10). 닉네임이 바뀔 때마다 1행 추가되는 append-only 기록.
 *
 * <p>기록 주체는 service-user(원본), admin-server는 조회만 한다. 스키마는 admin-server(Flyway)가 소유.
 */
@Entity
@Table(name = "nickname_change_history", indexes = {
        @Index(name = "idx_nickname_history_member", columnList = "member_id, changed_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NicknameChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "old_nickname", length = 20)
    private String oldNickname;

    @Column(name = "new_nickname", nullable = false, length = 20)
    private String newNickname;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static NicknameChangeHistory of(Long memberId, String oldNickname,
                                           String newNickname, LocalDateTime changedAt) {
        NicknameChangeHistory h = new NicknameChangeHistory();
        h.memberId = memberId;
        h.oldNickname = oldNickname;
        h.newNickname = newNickname;
        h.changedAt = changedAt;
        h.createdAt = changedAt;
        return h;
    }
}
