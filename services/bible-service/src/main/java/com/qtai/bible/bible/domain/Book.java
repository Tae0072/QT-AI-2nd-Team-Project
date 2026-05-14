package com.qtai.bible.bible.domain;

import com.qtai.bible.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 성경 책 메타 (66권). 02_ERD §3.2.
 */
@Entity
@Table(name = "bible_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_code", length = 8, unique = true, nullable = false)
    private String bookCode;       // GEN, EXO, ...

    @Column(name = "name_kr", length = 50, nullable = false)
    private String nameKr;

    @Column(name = "name_en", length = 50, nullable = false)
    private String nameEn;

    @Column(name = "testament", length = 3, nullable = false)
    private String testament;      // OT / NT

    @Column(name = "ordinal", nullable = false)
    private Integer ordinal;       // 1~66
}
