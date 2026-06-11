package com.qtai.domain.ai.internal;

import java.util.List;

final class AiReviewReferenceBookCatalog {

    private static final List<Book> BOOKS = List.of(
            new Book("GEN", "창세기", 1),
            new Book("EXO", "출애굽기", 2),
            new Book("LEV", "레위기", 3),
            new Book("NUM", "민수기", 4),
            new Book("DEU", "신명기", 5),
            new Book("JOS", "여호수아", 6),
            new Book("JDG", "사사기", 7),
            new Book("RUT", "룻기", 8),
            new Book("1SA", "사무엘상", 9),
            new Book("2SA", "사무엘하", 10),
            new Book("1KI", "열왕기상", 11),
            new Book("2KI", "열왕기하", 12),
            new Book("1CH", "역대상", 13),
            new Book("2CH", "역대하", 14),
            new Book("EZR", "에스라", 15),
            new Book("NEH", "느헤미야", 16),
            new Book("EST", "에스더", 17),
            new Book("JOB", "욥기", 18),
            new Book("PSA", "시편", 19),
            new Book("PRO", "잠언", 20),
            new Book("ECC", "전도서", 21),
            new Book("SNG", "아가", 22),
            new Book("ISA", "이사야", 23),
            new Book("JER", "예레미야", 24),
            new Book("LAM", "예레미야애가", 25),
            new Book("EZK", "에스겔", 26),
            new Book("DAN", "다니엘", 27),
            new Book("HOS", "호세아", 28),
            new Book("JOL", "요엘", 29),
            new Book("AMO", "아모스", 30),
            new Book("OBA", "오바댜", 31),
            new Book("JON", "요나", 32),
            new Book("MIC", "미가", 33),
            new Book("NAM", "나훔", 34),
            new Book("HAB", "하박국", 35),
            new Book("ZEP", "스바냐", 36),
            new Book("HAG", "학개", 37),
            new Book("ZEC", "스가랴", 38),
            new Book("MAL", "말라기", 39),
            new Book("MAT", "마태복음", 40),
            new Book("MRK", "마가복음", 41),
            new Book("LUK", "누가복음", 42),
            new Book("JHN", "요한복음", 43),
            new Book("ACT", "사도행전", 44),
            new Book("ROM", "로마서", 45),
            new Book("1CO", "고린도전서", 46),
            new Book("2CO", "고린도후서", 47),
            new Book("GAL", "갈라디아서", 48),
            new Book("EPH", "에베소서", 49),
            new Book("PHP", "빌립보서", 50),
            new Book("COL", "골로새서", 51),
            new Book("1TH", "데살로니가전서", 52),
            new Book("2TH", "데살로니가후서", 53),
            new Book("1TI", "디모데전서", 54),
            new Book("2TI", "디모데후서", 55),
            new Book("TIT", "디도서", 56),
            new Book("PHM", "빌레몬서", 57),
            new Book("HEB", "히브리서", 58),
            new Book("JAS", "야고보서", 59),
            new Book("1PE", "베드로전서", 60),
            new Book("2PE", "베드로후서", 61),
            new Book("1JN", "요한일서", 62),
            new Book("2JN", "요한이서", 63),
            new Book("3JN", "요한삼서", 64),
            new Book("JUD", "유다서", 65),
            new Book("REV", "요한계시록", 66)
    );

    private AiReviewReferenceBookCatalog() {
    }

    static List<Book> books() {
        return BOOKS;
    }

    record Book(
            String code,
            String koreanName,
            int displayOrder
    ) {
    }
}
