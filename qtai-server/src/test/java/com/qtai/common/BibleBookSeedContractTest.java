package com.qtai.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BibleBookSeedContractTest {

    @Test
    @DisplayName("성경 권 seed는 팀 명세 기준 OLD/NEW testament 값을 사용한다")
    void bibleBookSeedUsesOldNewTestamentValues() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V7__seed_bible_books.sql"));

        assertThat(migration).contains("'OLD'", "'NEW'");
        assertThat(migration).doesNotContain("'OT'", "'NT'");
    }
}
