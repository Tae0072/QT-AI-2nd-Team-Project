package com.qtai.domain.qt.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 오늘 QT 본문 TTS 음성 캐시. (QT 본문, 목소리) 조합당 1개 음성을 보관한다.
 * 본문(한글 절 범위)만 읽으므로 절별이 아니라 본문 단위로 저장한다.
 */
@Entity
@Table(name = "qt_passage_audio", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"qt_passage_id", "voice"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QtPassageAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qt_passage_id", nullable = false)
    private Long qtPassageId;

    @Column(name = "voice", nullable = false, length = 60)
    private String voice;

    @Column(name = "mime_type", nullable = false, length = 60)
    private String mimeType;

    @Column(name = "byte_size", nullable = false)
    private Long byteSize;

    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "audio_data", nullable = false)
    private byte[] audioData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static QtPassageAudio of(Long qtPassageId, String voice, String mimeType,
                                    byte[] audioData, LocalDateTime createdAt) {
        QtPassageAudio a = new QtPassageAudio();
        a.qtPassageId = qtPassageId;
        a.voice = voice;
        a.mimeType = mimeType;
        a.audioData = audioData;
        a.byteSize = (long) (audioData == null ? 0 : audioData.length);
        a.createdAt = createdAt;
        return a;
    }
}
