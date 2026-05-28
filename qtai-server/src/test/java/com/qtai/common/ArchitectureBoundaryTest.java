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
        assertThat(violationsFor("bible")).isEmpty();
    }

    @Test
    @DisplayName("다른 도메인은 study internal/web 패키지를 직접 import하지 않는다")
    void otherDomains_doNotImportStudyInternalOrWeb() throws IOException {
        assertThat(violationsFor("study")).isEmpty();
    }

    @Test
    @DisplayName("study 도메인은 qt internal/web 패키지를 직접 import하지 않는다")
    void studyDomain_doesNotImportQtInternalOrWebPackages() throws IOException {
        assertThat(violationsForDomain("study", "qt")).isEmpty();
    }

    @Test
    @DisplayName("qt 도메인은 study internal/web 패키지를 직접 import하지 않는다")
    void qtDomain_doesNotImportStudyInternalOrWebPackages() throws IOException {
        assertThat(violationsForDomain("qt", "study")).isEmpty();
    }

    @Test
    @DisplayName("note 도메인은 bible/qt/sharing internal/web 패키지를 직접 import하지 않는다")
    void noteDomain_doesNotImportOtherInternalOrWebPackages() throws IOException {
        Path noteRoot = Path.of("src/main/java/com/qtai/domain/note");

        try (var stream = Files.walk(noteRoot)) {
            List<String> violations = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> importsOf(path).stream())
                    .filter(line -> line.contains("com.qtai.domain.bible.internal")
                            || line.contains("com.qtai.domain.bible.web")
                            || line.contains("com.qtai.domain.qt.internal")
                            || line.contains("com.qtai.domain.qt.web")
                            || line.contains("com.qtai.domain.sharing.internal")
                            || line.contains("com.qtai.domain.sharing.web"))
                    .toList();
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("member web은 note api만 import하고 note internal 패키지를 직접 import하지 않는다")
    void memberWeb_importsOnlyNoteApiBoundary() throws IOException {
        Path memberWebRoot = Path.of("src/main/java/com/qtai/domain/member/web");

        try (var stream = Files.walk(memberWebRoot)) {
            List<String> violations = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> importsOf(path).stream())
                    .filter(line -> line.contains("com.qtai.domain.note.internal"))
                    .toList();
            assertThat(violations).isEmpty();
        }
    }

    private static List<String> violationsFor(String domainName) throws IOException {
        Path domainRoot = Path.of("src/main/java/com/qtai/domain");

        try (var stream = Files.walk(domainRoot)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(domainRoot.resolve(domainName)))
                    .flatMap(path -> importsOf(path).stream())
                    .filter(line -> line.contains("com.qtai.domain." + domainName + ".internal")
                            || line.contains("com.qtai.domain." + domainName + ".web"))
                    .toList();
        }
    }

    private static List<String> violationsForDomain(String sourceDomain, String forbiddenDomain) throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/qtai/domain").resolve(sourceDomain);

        try (var stream = Files.walk(sourceRoot)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> importsOf(path).stream())
                    .filter(line -> line.contains("com.qtai.domain." + forbiddenDomain + ".internal")
                            || line.contains("com.qtai.domain." + forbiddenDomain + ".web"))
                    .toList();
        }
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
