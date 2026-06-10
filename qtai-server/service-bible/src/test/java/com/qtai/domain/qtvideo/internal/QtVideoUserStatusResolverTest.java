package com.qtai.domain.qtvideo.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qtai.domain.qtvideo.api.dto.QtVideoUserStatus;
import com.qtai.support.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

class QtVideoUserStatusResolverTest {

    @Test
    @DisplayName("사용자 노출 후보 상태는 승인, 숨김, 실패 순서이다")
    void userStatusCandidateStatuses_areOrdered() {
        assertEquals(
                List.of(QtVideoClipStatus.APPROVED, QtVideoClipStatus.HIDDEN, QtVideoClipStatus.FAILED),
                QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES);
        assertFalse(QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES.contains(QtVideoClipStatus.PENDING));
    }

    @Test
    @DisplayName("DB 클립 상태를 사용자 노출 상태로 변환한다")
    void toUserStatus_mapsClipStatus() {
        assertEquals(QtVideoUserStatus.READY, QtVideoUserStatusResolver.toUserStatus(QtVideoClipStatus.APPROVED));
        assertEquals(QtVideoUserStatus.FAILED, QtVideoUserStatusResolver.toUserStatus(QtVideoClipStatus.FAILED));
        assertEquals(QtVideoUserStatus.DISABLED, QtVideoUserStatusResolver.toUserStatus(QtVideoClipStatus.HIDDEN));
        assertEquals(QtVideoUserStatus.MISSING, QtVideoUserStatusResolver.toUserStatus(QtVideoClipStatus.PENDING));
    }

    @Test
    @DisplayName("사용자 노출 클립은 승인, 숨김, 실패 순서로 선택한다")
    void chooseUserStatusClip_prefersApprovedThenHiddenThenFailed() {
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip failed = TestEntityFactory.qtVideoClip(
                10L,
                4L,
                sourceVideo,
                "https://cdn.example.com/failed.mp4",
                QtVideoClipStatus.FAILED);
        QtVideoClip hidden = TestEntityFactory.qtVideoClip(
                11L,
                4L,
                sourceVideo,
                "https://cdn.example.com/hidden.mp4",
                QtVideoClipStatus.HIDDEN);
        QtVideoClip approved = TestEntityFactory.qtVideoClip(
                12L,
                4L,
                sourceVideo,
                "https://cdn.example.com/approved.mp4",
                QtVideoClipStatus.APPROVED);

        Optional<QtVideoClip> chosen = QtVideoUserStatusResolver.chooseUserStatusClip(List.of(failed, hidden, approved));

        assertTrue(chosen.isPresent());
        assertEquals(approved, chosen.get());
    }

    @Test
    @DisplayName("승인 클립이 없으면 숨김 클립을 실패 클립보다 먼저 선택한다")
    void chooseUserStatusClip_prefersHiddenOverFailed() {
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip failed = TestEntityFactory.qtVideoClip(
                10L,
                4L,
                sourceVideo,
                "https://cdn.example.com/failed.mp4",
                QtVideoClipStatus.FAILED);
        QtVideoClip hidden = TestEntityFactory.qtVideoClip(
                11L,
                4L,
                sourceVideo,
                "https://cdn.example.com/hidden.mp4",
                QtVideoClipStatus.HIDDEN);

        Optional<QtVideoClip> chosen = QtVideoUserStatusResolver.chooseUserStatusClip(List.of(failed, hidden));

        assertTrue(chosen.isPresent());
        assertEquals(hidden, chosen.get());
    }
}
