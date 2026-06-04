package com.qtai.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DummyBibleSeedContractTest {

    @Test
    @DisplayName("고린도전서 1장 10-17절 앱 확인용 더미 seed는 삭제 안내 주석과 실제 번역본이 아닌 문구만 포함한다")
    void dummySeedContainsRemovalNoticeAndOnlyPlaceholderText() throws Exception {
        String fixture = Files.readString(Path.of(
                "src/test/resources/db/fixtures/dummy_today_qt_1co_verses.sql"));

        assertThat(fixture).contains("운영 Flyway migration이 아니라 테스트/로컬 수동 확인 전용 fixture");
        assertThat(fixture).contains("이승욱님 seed 실제데이터입력시 해당 더미데이터 삭제바람");
        assertThat(fixture).contains("한글 테스트 본문 10", "English dummy verse 10");
        assertThat(fixture).contains("2026-06-02", "1CO", "1 Corinthians");
        assertThat(fixture)
                .as("금지 번역본 본문은 더미 seed에 포함하지 않는다")
                .doesNotContain("개역개정", "ESV", "NIV"); // 금지 번역본 검증

        assertThat(Files.exists(Path.of(
                "src/main/resources/db/migration/V23__seed_dummy_today_qt_1co_verses.sql")))
                .as("운영 Flyway migration에는 더미 seed를 두지 않는다")
                .isFalse();
    }
}
