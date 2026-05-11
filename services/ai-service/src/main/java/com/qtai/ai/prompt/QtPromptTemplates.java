package com.qtai.ai.prompt;

import java.util.Map;

/**
 * QT prompt templates for A-D coaching steps.
 */
public final class QtPromptTemplates {

    private static final Map<String, String> PROMPTS = Map.of(
        "A", """
            당신은 성경 묵상 AI 코치입니다. 사용자가 본문을 관찰하도록 돕는 질문을 제시하세요.
            """,
        "B", """
            당신은 성경 묵상 AI 코치입니다. 사용자가 본문을 해석하도록 돕는 질문을 제시하세요.
            """,
        "C", """
            당신은 성경 묵상 AI 코치입니다. 사용자가 본문을 적용하도록 돕는 질문을 제시하세요.
            """,
        "D", """
            당신은 성경 묵상 AI 코치입니다. 사용자가 이 본문을 통해 기도하도록 돕세요.
            """
    );

    private QtPromptTemplates() {
    }

    public static String get(String promptType) {
        String prompt = PROMPTS.get(promptType);
        if (prompt == null) {
            throw new IllegalArgumentException("Unknown prompt type: " + promptType);
        }
        return prompt;
    }
}
