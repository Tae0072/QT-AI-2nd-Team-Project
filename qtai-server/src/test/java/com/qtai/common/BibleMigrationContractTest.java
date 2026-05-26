package com.qtai.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BibleMigrationContractTest {

    @Test
    @DisplayName("V2 bible migration은 ERD/API 기준 컬럼명과 절 좌표 unique 제약을 사용한다")
    void v2BibleMigration_usesCanonicalColumnNamesAndUniqueCoordinate() throws IOException {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V2__create_bible.sql"));

        assertThat(migration).contains("chapter_no", "verse_no", "korean_text", "english_text");
        assertThat(migration).contains("UNIQUE KEY uk_bible_verse_coord (book_id, chapter_no, verse_no)");
        assertThat(migration).doesNotContain("krv_" + "text", "kjv_" + "text");
    }
}
