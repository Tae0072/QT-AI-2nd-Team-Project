package com.qtai.domain.ai.internal;

import java.util.ArrayList;
import java.util.List;

class AiReviewReferenceTextQualityAnalyzer {

    private static final int MIN_TEXT_LENGTH = 40;
    private static final int MAX_TEXT_LENGTH = 12_000;
    private static final double QUESTION_MARK_NEEDS_REVIEW_RATIO = 0.03;
    private static final double QUESTION_MARK_UNUSABLE_RATIO = 0.12;
    private static final double MOJIBAKE_NEEDS_REVIEW_RATIO = 0.01;
    private static final double MOJIBAKE_UNUSABLE_RATIO = 0.08;

    QualityResult analyze(String text) {
        String normalized = text == null ? "" : text.strip();
        int textLength = normalized.length();
        int replacementCharCount = count(normalized, '�');
        int questionMarkCount = count(normalized, '?');
        int suspiciousMojibakeCount = suspiciousMojibakeCount(normalized);
        int hangulCount = hangulCount(normalized);

        double questionMarkRatio = ratio(questionMarkCount, textLength);
        double suspiciousMojibakeRatio = ratio(suspiciousMojibakeCount, textLength);
        double hangulRatio = ratio(hangulCount, textLength);
        List<String> reasons = new ArrayList<>();
        QualityStatus status = QualityStatus.USABLE;

        if (textLength < MIN_TEXT_LENGTH) {
            reasons.add("TEXT_TOO_SHORT");
            status = QualityStatus.UNUSABLE;
        }
        if (textLength > MAX_TEXT_LENGTH) {
            reasons.add("TEXT_TOO_LONG");
            status = max(status, QualityStatus.NEEDS_REVIEW);
        }
        if (replacementCharCount > 0) {
            reasons.add("REPLACEMENT_CHAR_FOUND");
            status = max(status, replacementCharCount > 10 ? QualityStatus.UNUSABLE : QualityStatus.NEEDS_REVIEW);
        }
        if (questionMarkRatio >= QUESTION_MARK_UNUSABLE_RATIO) {
            reasons.add("QUESTION_MARK_RATIO_HIGH");
            status = max(status, QualityStatus.UNUSABLE);
        } else if (questionMarkRatio >= QUESTION_MARK_NEEDS_REVIEW_RATIO) {
            reasons.add("QUESTION_MARK_RATIO_REVIEW");
            status = max(status, QualityStatus.NEEDS_REVIEW);
        }
        if (suspiciousMojibakeRatio >= MOJIBAKE_UNUSABLE_RATIO) {
            reasons.add("MOJIBAKE_RATIO_HIGH");
            status = max(status, QualityStatus.UNUSABLE);
        } else if (suspiciousMojibakeRatio >= MOJIBAKE_NEEDS_REVIEW_RATIO) {
            reasons.add("MOJIBAKE_RATIO_REVIEW");
            status = max(status, QualityStatus.NEEDS_REVIEW);
        }
        if (textLength >= MIN_TEXT_LENGTH && hangulRatio < 0.05) {
            reasons.add("HANGUL_RATIO_LOW");
            status = max(status, QualityStatus.NEEDS_REVIEW);
        }

        return new QualityResult(
                status,
                replacementCharCount,
                questionMarkRatio,
                suspiciousMojibakeRatio,
                hangulRatio,
                textLength,
                reasons
        );
    }

    private static int count(String text, char target) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == target) {
                count++;
            }
        }
        return count;
    }

    private static int suspiciousMojibakeCount(String text) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (ch == '占' || ch == '竊' || ch == '�') {
                count++;
            }
        }
        return count;
    }

    private static int hangulCount(String text) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (ch >= '가' && ch <= '힣') {
                count++;
            }
        }
        return count;
    }

    private static double ratio(int count, int textLength) {
        if (textLength == 0) {
            return 0.0;
        }
        return (double) count / textLength;
    }

    private static QualityStatus max(QualityStatus left, QualityStatus right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    enum QualityStatus {
        USABLE,
        NEEDS_REVIEW,
        UNUSABLE
    }

    record QualityResult(
            QualityStatus status,
            int replacementCharCount,
            double questionMarkRatio,
            double suspiciousMojibakeRatio,
            double hangulRatio,
            int textLength,
            List<String> reasons
    ) {

        QualityResult {
            reasons = List.copyOf(reasons);
        }

        QualityResult withReason(String reason, QualityStatus minimumStatus) {
            List<String> updatedReasons = new ArrayList<>(reasons);
            if (!updatedReasons.contains(reason)) {
                updatedReasons.add(reason);
            }
            return new QualityResult(
                    max(status, minimumStatus),
                    replacementCharCount,
                    questionMarkRatio,
                    suspiciousMojibakeRatio,
                    hangulRatio,
                    textLength,
                    updatedReasons
            );
        }
    }
}
