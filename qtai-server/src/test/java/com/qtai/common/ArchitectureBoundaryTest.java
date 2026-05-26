package com.qtai.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureBoundaryTest {

    @Test
    @DisplayName("다른 도메인은 bible internal/web 패키지를 직접 import하지 않는다")
    void otherDomains_doNotImportBibleInternalOrWeb() throws IOException {
        Path domainRoot = Path.of("src/main/java/com/qtai/domain");

        List<String> violations;
        try (var stream = Files.walk(domainRoot)) {
            violations = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(domainRoot.resolve("bible")))
                    .flatMap(path -> importsOf(path).stream())
                    .filter(line -> line.contains("com.qtai.domain.bible.internal")
                            || line.contains("com.qtai.domain.bible.web"))
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    private static List<String> importsOf(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .filter(line -> line.startsWith("import "))
                    .map(line -> path + ": " + line)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
