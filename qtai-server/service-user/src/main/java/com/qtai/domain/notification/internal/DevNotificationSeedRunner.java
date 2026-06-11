package com.qtai.domain.notification.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * dev 전용 알림 더미 시드 러너 — 시연/발표 때 알림 목록(M-02)이 비어 보이지 않도록 예시 알림을 넣는다.
 *
 * <p>{@code DevMemberSeedRunner}와 같은 게이트(dev 프로파일 + {@code qtai.security.dev-bypass=true})를
 * 사용하므로 운영(prod)에는 절대 실리지 않는다. 대상 회원은 dev 웹 우회 로그인 기본 회원
 * ({@code qtai.dev.notification-seed-member-id}, 기본 1)이다.
 *
 * <p>멱등성: 모든 시드는 고정 {@code event_key}({@value #SEED_KEY_PREFIX} + n)를 가지므로
 * (member_id, event_key) UNIQUE 제약과 선행 존재 검사로 재기동 시 중복 삽입되지 않는다.
 * 대상 회원이 아직 없으면(FK 위반) 시드를 건너뛰고 로그만 남긴다.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "qtai.security.dev-bypass", havingValue = "true")
class DevNotificationSeedRunner implements ApplicationRunner {

    static final String SEED_KEY_PREFIX = "dev-seed-notification-";

    private final NotificationRepository notificationRepository;
    private final Clock clock;
    private final long seedMemberId;

    DevNotificationSeedRunner(NotificationRepository notificationRepository,
                              Clock clock,
                              @Value("${qtai.dev.notification-seed-member-id:1}") long seedMemberId) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
        this.seedMemberId = seedMemberId;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (notificationRepository.existsByMemberIdAndEventKey(seedMemberId, SEED_KEY_PREFIX + 1)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);

        Notification todayQt = seed(1, NotificationType.NOTICE,
                "오늘의 QT가 준비됐어요",
                "고린도전서 6:1-11 — 부끄러운 송사. 오늘도 말씀과 함께 하루를 시작해 보세요.",
                now.minusHours(4));
        Notification comment = seed(2, NotificationType.COMMENT,
                "내 나눔에 새 댓글이 달렸어요",
                "묵상하는곰: 저도 같은 구절에서 멈췄어요. 나눠주셔서 감사합니다.",
                now.minusHours(1));
        Notification like = seed(3, NotificationType.LIKE,
                "내 나눔을 5명이 공감해요",
                "'이기는 것보다 회복이 먼저' 나눔이 공감을 받고 있어요.",
                now.minusDays(1));
        Notification feature = seed(4, NotificationType.NOTICE,
                "새 기능 안내",
                "이제 묵상 달력에서 한 달의 기록을 한눈에 볼 수 있어요.",
                now.minusDays(3));
        Notification report = seed(5, NotificationType.REPORT_RESULT,
                "신고 처리 결과 안내",
                "회원님이 신고하신 게시물이 커뮤니티 가이드 위반으로 비공개 처리됐어요.",
                now.minusDays(5));

        // 발표 화면 구성: 최근 2건(미읽음) + 과거 3건(읽음) — 미읽음 필터 토글 시연이 가능하다.
        like.markAsRead(clock);
        feature.markAsRead(clock);
        report.markAsRead(clock);

        try {
            notificationRepository.saveAll(List.of(todayQt, comment, like, feature, report));
            log.info("dev 알림 더미 시드 완료: memberId={}, count=5", seedMemberId);
        } catch (DataIntegrityViolationException e) {
            // 시드 회원 미존재(FK) 또는 동시 기동 경합 — 시연용 데이터이므로 건너뛴다.
            log.info("dev 알림 더미 시드 건너뜀: memberId={} (회원 미존재 또는 중복)", seedMemberId);
        }
    }

    private Notification seed(int n, NotificationType type, String title, String body,
                              LocalDateTime createdAt) {
        return Notification.builder()
                .memberId(seedMemberId)
                .type(type)
                .title(title)
                .body(body)
                .eventKey(SEED_KEY_PREFIX + n)
                .createdAt(createdAt)
                .build();
    }
}
