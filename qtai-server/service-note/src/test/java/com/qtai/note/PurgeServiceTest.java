package com.qtai.note;

import com.qtai.domain.note.internal.NotePurgeService;
import com.qtai.domain.report.internal.ReportPurgeService;
import com.qtai.domain.sharing.internal.SharingPurgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 회원 보존기간 만료 정리(Purge) 서비스 단위 테스트.
 *
 * <p>JdbcTemplate 직접 호출 코드라 회귀 위험이 높다. 각 도메인이 자기 테이블만, 올바른 순서로
 * 삭제하고 삭제 건수 합계를 반환하는지 검증한다(JDBC 실행은 mock으로 격리).
 */
@ExtendWith(MockitoExtension.class)
class PurgeServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Test
    void report_정리는_자기_테이블만_삭제하고_건수를_반환한다() {
        when(jdbc.update(eq("DELETE FROM reports WHERE reporter_member_id = ?"), eq(5L))).thenReturn(2);

        assertThat(new ReportPurgeService(jdbc).purgeByMemberId(5L)).isEqualTo(2);
    }

    @Test
    void note_정리는_이벤트_구절연결_노트본체_순서로_삭제하고_합계를_반환한다() {
        when(jdbc.update(contains("journal_events"), eq(7L), eq(7L))).thenReturn(1);
        when(jdbc.update(contains("note_verses"), eq(7L))).thenReturn(2);
        when(jdbc.update(eq("DELETE FROM notes WHERE member_id = ?"), eq(7L))).thenReturn(3);

        assertThat(new NotePurgeService(jdbc).purgeByMemberId(7L)).isEqualTo(6);
        verify(jdbc).update(contains("journal_events"), eq(7L), eq(7L));
        verify(jdbc).update(eq("DELETE FROM notes WHERE member_id = ?"), eq(7L));
    }

    @Test
    void sharing_정리는_댓글이_없으면_좋아요와_나눔글만_삭제한다() {
        when(jdbc.update(contains("post_likes"), eq(9L), eq(9L))).thenReturn(4);
        // 삭제 대상 댓글이 없으면 댓글 트리 삭제는 0건으로 조기 종료한다.
        when(jdbc.queryForList(contains("FROM comments"), eq(Long.class), eq(9L), eq(9L)))
                .thenReturn(Collections.<Long>emptyList());
        when(jdbc.update(eq("DELETE FROM sharing_posts WHERE member_id = ?"), eq(9L))).thenReturn(1);

        assertThat(new SharingPurgeService(jdbc).purgeByMemberId(9L)).isEqualTo(5);
        verify(jdbc).update(contains("post_likes"), eq(9L), eq(9L));
    }
}
