package com.qtai.domain.music.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배경음악 시드 러너 (클래스패스 번들 음원 → DB).
 *
 * <p>음원 mp3는 레포에 {@code src/main/resources/seed/music/{bgm,hymn}/} 로 포함되어
 * 빌드 산출물(jar)에 패키징된다. 로컬 OS 경로에 의존하지 않으므로 어느 환경에서든
 * git pull 후 실행만 하면 {@code music_tracks} 가 비어 있을 때 자동 적재된다.
 *
 * <p>멱등: 음원이 이미 있으면 건너뛴다. H2 in-memory(로컬 기본)는 재시작마다
 * 비어 있어 재적재되고, MySQL은 최초 1회만 적재된다.
 *
 * <p>{@code qtai.music.seed.enabled=false} 면 비활성(테스트에서 끈다). 기본은 ON.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "qtai.music.seed.enabled", havingValue = "true", matchIfMissing = true)
public class MusicSeedRunner implements ApplicationRunner {

    private static final String BGM_LOCATION = "classpath*:seed/music/bgm/*.mp3";
    private static final String HYMN_LOCATION = "classpath*:seed/music/hymn/*.mp3";

    private final MusicTrackRepository musicTrackRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existing = musicTrackRepository.count();
        if (existing > 0) {
            log.info("[music-seed] 이미 음원이 있어 시드를 건너뜁니다. count={}", existing);
            return;
        }
        int sort = 0;
        sort = seed(BGM_LOCATION, MusicCategory.BGM, sort);
        sort = seed(HYMN_LOCATION, MusicCategory.HYMN, sort);
        log.info("[music-seed] 시드 완료. 총 {}곡", musicTrackRepository.count());
    }

    private int seed(String locationPattern, MusicCategory category, int startSort) {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(locationPattern);
        } catch (IOException e) {
            log.warn("[music-seed] 리소스 조회 실패: {} — {}", locationPattern, e.getMessage());
            return startSort;
        }
        // 파일명 기준 정렬로 안정적 순서 보장
        Arrays.sort(resources, Comparator.comparing(r -> {
            String n = r.getFilename();
            return n != null ? n : "";
        }));

        int sort = startSort;
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                byte[] data = in.readAllBytes();
                String title = filename.toLowerCase().endsWith(".mp3")
                        ? filename.substring(0, filename.length() - 4)
                        : filename;
                MusicTrack track = MusicTrack.builder()
                        .title(title)
                        .category(category)
                        .mimeType("audio/mpeg")
                        .byteSize((long) data.length)
                        .sortOrder(sort++)
                        .enabled(true)
                        .licenseNote("로열티프리/직접제작 (번들 시드)")
                        .audioData(data)
                        .build();
                musicTrackRepository.save(track);
                log.info("[music-seed] 적재: [{}] {} ({} bytes)", category, title, data.length);
            } catch (IOException e) {
                log.warn("[music-seed] 음원 적재 실패: {} — {}", filename, e.getMessage());
            }
        }
        return sort;
    }
}
