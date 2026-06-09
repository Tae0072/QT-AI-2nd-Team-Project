package com.qtai.domain.sharing.client.notification;

import com.qtai.domain.notification.api.SendNotificationUseCase;
import com.qtai.domain.notification.api.dto.NotificationSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * notification 도메인 {@link SendNotificationUseCase}의 service-note 임시 구현(Mock).
 *
 * <p>MSA 분리 기준(CLAUDE.md §4): notification은 service-user 소관이라 service-note에서는 api 계약
 * 타입만 가져와 client 어댑터로 임시 구현한다. 통합 시 이 Mock을 RestClient 호출 어댑터로 교체한다.
 *
 * <p>통합 전까지는 발송을 수행하지 않는다(no-op). 로그에는 제목/본문 등 사용자 콘텐츠를 남기지 않고
 * (CLAUDE.md §9), 관측을 위해 알림 유형만 debug로 남긴다.
 */
@Slf4j
@Component("sharingNotificationUseCaseMock")
public class SendNotificationUseCaseMock implements SendNotificationUseCase {

    @Override
    public void send(NotificationSendRequest request) {
        log.debug("[mock] 알림 발송 생략(통합 전). type={}", request == null ? null : request.type());
    }
}
