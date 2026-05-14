package com.qtai.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 큐티 가이드 A~D 프롬프트 템플릿.
 *
 * <p>구조 (09_AI_프롬프트_운영_가이드 §3):
 * - A: 본문 관찰 (단어/문맥)
 * - B: 본문 이해 (배경/의도)
 * - C: 본문 적용 (오늘 내 삶)
 * - D: 기도/결단 보조
 *
 * <p>TODO(강상민): DB 적재된 PROMPT_TEMPLATES row 우선, 없으면 본 클래스의 fallback.
 * 응답 말투는 차분한 정보 전달형이고, 사용자 질문이 본문 범위를 벗어나면 거절 가이드 포함.
 */
@Component
public class QtPromptTemplates {

    private static final String SYSTEM_BASE = """
            당신은 차분한 묵상 도우미입니다. 주어진 성경 본문(book/chapter/verse)에만 근거해 답변합니다.
            본문에서 벗어난 질문, 신학적 단정, 점성/타로/예언은 정중히 거절하고 본문으로 돌립니다.
            사용자가 묵상에 집중할 수 있도록 짧고 구체적으로 답합니다.
            """;

    private static final Map<String, String> STEP_INSTRUCTIONS = Map.of(
            "A", "관찰 단계: 본문 단어와 표현을 차분히 설명하고, 사용자가 본문에서 무엇을 보았는지 짧게 묻습니다.",
            "B", "이해 단계: 본문이 쓰인 맥락과 의도를 사전 적재된 해설 범위 내에서 정리합니다.",
            "C", "적용 단계: 사용자가 자기 삶에 적용할 한 가지 질문을 던집니다. 답을 단정하지 않습니다.",
            "D", "기도/결단 단계: 사용자가 직접 기도문을 정리할 수 있도록 1~2문장 보조만 합니다."
    );

    public String systemPrompt(String guideStep) {
        String suffix = STEP_INSTRUCTIONS.getOrDefault(guideStep, "");
        return SYSTEM_BASE + "\n" + suffix;
    }

    public String userPromptWithContext(String passageRef, String passageKr, String passageEn,
                                         String explanationContext, String userMessage) {
        return """
               [본문]
               %s
               KR: %s
               EN: %s

               [출처 컨텍스트]
               %s

               [질문]
               %s
               """.formatted(passageRef, passageKr, passageEn, explanationContext, userMessage);
    }
}
