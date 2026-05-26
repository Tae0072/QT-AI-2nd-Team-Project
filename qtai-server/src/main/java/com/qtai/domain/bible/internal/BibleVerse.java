package com.qtai.domain.bible.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bible_verses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BibleVerse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Short bookId;

    @Column(nullable = false)
    private Short chapter;

    @Column(nullable = false)
    private Short verse;

    @Column(name = "krv_text", nullable = false, columnDefinition = "TEXT")
    private String krvText;

    @Column(name = "kjv_text", columnDefinition = "TEXT")
    private String kjvText;
}
