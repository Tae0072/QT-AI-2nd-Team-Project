package com.qtai.domain.bible.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bible_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BibleBook {

    @Id
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Testament testament;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "korean_name", nullable = false, length = 30)
    private String koreanName;

    @Column(name = "english_name", nullable = false, length = 50)
    private String englishName;

    @Column(name = "display_order", nullable = false, unique = true)
    private Short displayOrder;

    public enum Testament {
        OLD, NEW
    }
}
