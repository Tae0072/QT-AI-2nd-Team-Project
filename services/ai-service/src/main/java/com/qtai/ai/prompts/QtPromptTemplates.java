package com.qtai.ai.prompts;

import java.util.Map;

/**
 * 큐티 프롬프트 템플릿 (A~D 유형).
 *
 * A: 관찰형   — "이 텍스트에서 무엇을 볼 수 있나요?"
 * B: 해석형   — "이 말씀이 당시 듣는 이에게 어떤 의미였을까요?"
 * C: 적용형   — "오늘 이 말씀을 어떻게 적용해볼 수 있을까요?"
 * D: 기도형   — "이 말씀을 통해 기도하고 싶은 제목이 있나요?"
 */
public final class QtPromptTemplates {

    private QtPromptTemplates() {
        // Utility class
    }

    private static final Map<String, String> PROMPTS = Map.of(
        "A", """
            당신은 성경 묵상 AI 코치입니다. 사용자가 본문을 **관찰**하도록 돕는 질문을 제시하세요.
            구체적으로: 그 구절에서 보이는 인물, 장소, 사건, 반복되는 단어 등 "텍스트가 말하는 것"에 집중하세요.
            주석·신학적 해석은 아직 제시하지 마세요.
            """,
        "B", """
            당신은 성경 묵상 AI 코치입니다. 사용자가 본문을 **해석**하도록 돕는 질문을 제시하세요.
            구체적으로: 이 구절의 역사적·문화적 맥락, 당시 독자에게 주는 의미, 전후 문맥과의 관계를 다루세요.
            """,
        "C", """
            당신은 성경 묵상 AI 코치입니다. 사용자가 본문을 **적용**하도록 돕는 질문을 제시하세요.
            구체적으로: 오늘 사용자의 삶에서 실천 가능한 구체적 조치, 관계 회복, 마음가짐의 변화 등을 제안하세요.
            """,
        "D", """
            당신은 성경 묵상 AI 코치입니다. 사용자가 이 본문을 통해 **기도**하도록 돕세요.
            구체적으로: 감사, 회개, 간구, 중보 등 적절한 기도의 주제를 제안하세요.
            """
    );

    /**
     * 프롬프트 유형에 해당하는 시스템 프롬프트 반환.
     *
     * @param promptType A, B, C, D 중 하나
     * @throws IllegalArgumentException 알 수 없는 유형일 경우
     */
    public static String get(String promptType) {
        String prompt = PROMPTS.get(promptType);
        if (prompt == null) {
            throw new IllegalArgumentException("Unknown prompt type: " + promptType);
        }
        return prompt;
    }
}
