package com.qtai.bff.api;

import com.qtai.bff.usecase.PassageUseCase;
import com.qtai.bff.usecase.TodayQtUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * BFF QT/Passage 컨트롤러.
 *
 * <p>경로 (DECISIONS.md §3):
 * - GET /api/v1/qt/today                                            — 오늘 QT 미리보기 (소프트 로그인)
 * - GET /api/v1/passages/{bookCode}/{chapter}/{verse}               — 입체 묵상 화면 (소프트 로그인)
 */
@RestController
public class QtController {

    private final TodayQtUseCase todayQt;
    private final PassageUseCase passage;

    public QtController(TodayQtUseCase todayQt, PassageUseCase passage) {
        this.todayQt = todayQt;
        this.passage = passage;
    }

    @GetMapping("/api/v1/qt/today")
    public Map<String, Object> today() {
        return todayQt.execute();
    }

    @GetMapping("/api/v1/passages/{bookCode}/{chapter}/{verse}")
    public Map<String, Object> passage(@PathVariable String bookCode,
                                       @PathVariable Integer chapter,
                                       @PathVariable Integer verse,
                                       @RequestHeader(value = "Authorization", required = false) String bearer) {
        return passage.execute(bookCode, chapter, verse, bearer);
    }
}
