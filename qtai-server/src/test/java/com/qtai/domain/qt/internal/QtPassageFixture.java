package com.qtai.domain.qt.internal;

import java.time.LocalDate;

/**
 * QtPassage 테스트 전용 픽스처.
 *
 * <p>QtPassage가 protected 생성자만 가지므로 리플렉션으로 인스턴스를 생성한다.
 * 필드명 변경 시 이 클래스만 수정하면 테스트 전체에 반영된다.
 */
final class QtPassageFixture {

    private QtPassageFixture() {
        // 인스턴스 생성 방지
    }

    /**
     * 지정 날짜의 더미 QtPassage를 생성한다 (리플렉션으로 id 설정).
     *
     * @param id    엔티티 ID
     * @param date  QT 본문 날짜
     * @param title QT 본문 제목
     * @return 테스트용 QtPassage 인스턴스
     */
    static QtPassage createPassage(Long id, LocalDate date, String title) {
        try {
            QtPassage passage = QtPassage.class.getDeclaredConstructor().newInstance();

            setField(passage, passage.getClass().getSuperclass(), "id", id);
            setField(passage, passage.getClass(), "qtDate", date);
            setField(passage, passage.getClass(), "title", title);
            setField(passage, passage.getClass(), "bookId", (short) 1);
            setField(passage, passage.getClass(), "chapter", (short) 1);
            setField(passage, passage.getClass(), "startVerse", (short) 1);
            setField(passage, passage.getClass(), "endVerse", (short) 5);

            return passage;
        } catch (Exception e) {
            throw new RuntimeException("테스트 QtPassage 생성 실패", e);
        }
    }

    private static void setField(Object target, Class<?> declaringClass,
                                  String fieldName, Object value) throws Exception {
        var field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
