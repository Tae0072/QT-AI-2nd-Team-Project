package com.qtai.bible;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.qtai.domain.bible.internal.BibleService;
import com.qtai.domain.bible.web.BibleController;

/**
 * bible-service inbound(HTTP) 구성. {@code qtai.bible.inbound.enabled=true}일 때만 활성화.
 *
 * <p>skeleton 단계는 비활성(false)이라 트래픽을 받지 않는다(게이트웨이는 여전히 모놀리식으로 라우팅).
 * 활성화 시 BibleController가 {@code /api/v1/bible/**}를 처리하며, 구현(BibleService)은 persistence
 * 활성화를 전제로 한다. 트래픽 컷오버는 게이트웨이 라우트 분기(Inc2)에서.
 */
@Configuration
@ConditionalOnProperty(name = "qtai.bible.inbound.enabled", havingValue = "true")
@Import({
        BibleService.class,
        BibleController.class
})
public class BibleServiceInboundConfiguration {
}
