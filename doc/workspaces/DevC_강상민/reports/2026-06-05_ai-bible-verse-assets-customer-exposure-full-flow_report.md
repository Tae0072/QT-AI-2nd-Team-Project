# Report - 2026-06-05 ai-bible-verse-assets-customer-exposure-full-flow

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `test/ai-bible-verse-assets-customer-exposure-full-flow` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-bible-verse-assets-customer-exposure-full-flow.md` |
| 검증 대상 | 오늘 QT BIBLE_VERSE `EXPLANATION` 15건 고객 노출 |
| QT 범위 | `qtPassageId=4`, `1CO 3:1-15` |
| verseIds | `28412..28426` |
| index URI | `restricted://validation/index/reference-index.json` |
| index hash | `sha256:d50811d18c1d109a1ce0dc8331f25bb7daf249be1892d9ca742cbb64c20eca8b` |

## 결론

오늘 QT `1CO 3:1-15`의 15개 BIBLE_VERSE 산출물이 모두 고객 노출 상태임을 확인했다. 최종 `GetQtStudyContentUseCase.getStudyContent(4)` 응답 기준 고객 노출 수는 15건이며, `verse_explanations`에도 `APPROVED + ACTIVE` row 15건이 저장되어 있다.

실제 DeepSeek 생성과 layer 1/2 검수를 거쳐 승인된 누락 verse는 10건이다. 이후 재확인 실행에서는 15건이 이미 노출되어 `ALREADY_VISIBLE`로 집계되었고 추가 LLM 호출은 발생하지 않았다.

## 실행 조건

- dev MySQL: `qtai` database 사용
- Redis: running
- `qtai-server/restricted/validation/index/reference-index.json`: 존재 확인
- DeepSeek 모델: `deepseek-chat`
- 수동 실행 플래그: `QTAI_AI_CUSTOMER_EXPOSURE_SAMPLE=true`
- 외부 `qtai-server` 앱 컨테이너: manual test가 `QUEUED` job을 직접 claim하도록 중지한 상태에서 실행

초기 시도에서는 외부 앱 컨테이너의 background worker가 동일 dev DB의 `QUEUED` job을 먼저 claim해 일부 job이 실패했다. MySQL/Redis는 유지하고 앱 컨테이너만 중지한 뒤 재실행해 manual test가 전체 흐름을 소유하도록 했다.

## 실행 결과 요약

| 지표 | 값 |
| --- | --- |
| 최종 flowStatus | `FULLY_VISIBLE` |
| 최종 visible count | `15` |
| 최종 result count | `15` |
| 생성 완료 asset count | `15` |
| `verse_explanations` active approved count | `15` |
| validation layer 1 | asset 1-15 모두 `AUTO/PASSED` |
| validation layer 2 | asset 1-15 모두 `ADVISOR/PASSED`, `validationReferenceJobId=1` |
| 민감 저장 경계 | `ai_validation_logs.checklist_json` 민감 키 매치 `0` |

최종 summary 파일은 `qtai-server/build/ai-review-reference/bible-verse-customer-exposure-full-summary.json`에 생성되었으나 build 산출물이므로 stage하지 않았다.

## Verse별 상태

| verseId | assetId | 상태 | source |
| --- | ---: | --- | --- |
| 28412 | 1 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28413 | 2 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28414 | 6 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28415 | 7 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28416 | 8 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28417 | 9 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28418 | 10 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28419 | 3 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28420 | 4 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28421 | 11 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28422 | 12 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28423 | 13 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28424 | 14 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28425 | 15 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |
| 28426 | 5 | `APPROVED + ACTIVE` | `QT-AI DeepSeek` |

## 고객 노출 산출물

### verseId 28412

- summary: 바울은 고린도 교인들이 아직 성숙하지 못해 육신에 속한 자처럼 대우했다고 말합니다.
- explanation: 바울은 고린도 교인들을 '신령한 자'가 아닌 '육신에 속한 자', 즉 그리스도 안에서 어린 아이들처럼 대했다고 고백합니다. 이는 그들의 신앙이 아직 성숙하지 못했음을 의미하며, 바울이 그들에게 더 깊은 영적 진리를 가르칠 수 없었던 이유를 설명합니다.

### verseId 28413

- summary: 바울은 고린도 교인들이 아직 영적으로 성숙하지 못해 단단한 가르침(밥)이 아닌 기초적인 가르침(젖)만 받을 수 있었다고 말합니다.
- explanation: 이 구절에서 바울은 고린도 교인들의 영적 미성숙을 지적합니다. '젖'은 기독교 신앙의 기본 원리(예: 회개, 믿음, 세례)를, '밥'은 더 깊은 영적 진리(예: 성령의 역사, 교회의 본질)를 상징합니다. 바울은 그들이 아직도 분열과 시기로 인해 단단한 가르침을 감당할 준비가 되지 않았다고 안타까워합니다.

### verseId 28414

- summary: 바울은 고린도 교회 성도들이 아직도 성숙하지 못하고 세상적인 방식으로 행동하고 있다고 지적합니다.
- explanation: 이 구절에서 바울은 고린도 교회 안에 있는 시기와 다툼을 언급하며, 이러한 모습이 그들이 아직도 육신에 속해 있음을 증거한다고 말합니다. '육신에 속했다'는 것은 하나님의 성령의 인도함을 받지 않고 인간의 본성과 세상적인 가치관에 따라 살아간다는 의미입니다. 바울은 그들이 '사람을 따라 행한다'고 표현하며, 그리스도인이 아닌 일반 사람들과 다를 바 없는 삶을 살고 있음을 안타까워합니다.

### verseId 28415

- summary: 바울은 고린도 교회 성도들이 자신과 아볼로를 따르는 파벌로 나뉜 것을 지적하며, 이러한 행동이 육신에 속한 모습이라고 꾸짖습니다.
- explanation: 고린도 교회 안에서 어떤 사람들은 '나는 바울파다', 다른 사람들은 '나는 아볼로파다'라고 말하며 서로 나뉘었습니다. 바울은 이렇게 사람을 중심으로 파벌을 만드는 것이 성숙한 신자의 모습이 아니라, 아직 육신에 속한 어린아이 같은 신앙이라고 지적합니다. 중요한 것은 누가 가르치느냐가 아니라, 그리스도와 복음이어야 합니다.

### verseId 28416

- summary: 바울은 자신과 아볼로가 단지 하나님의 일꾼일 뿐이라고 설명합니다.
- explanation: 이 구절에서 바울은 고린도 교회 성도들이 자신이나 아볼로를 지나치게 높이는 것을 경계합니다. 그는 자신과 아볼로가 각자 주님께서 주신 역할을 따라 사람들이 믿음을 갖도록 도운 '사역자'(일꾼)에 불과하다고 말합니다. 중요한 것은 사람이 아니라, 그들을 통해 일하시는 하나님이라는 점을 강조합니다.

### verseId 28417

- summary: 바울은 자신과 아볼로의 역할을 비유로 설명하며, 모든 성장의 근원이 하나님이심을 강조합니다.
- explanation: 이 구절은 사도 바울이 고린도 교회의 분열 문제를 다루는 가운데 나옵니다. 바울은 자신이 복음을 처음 전하여 '심는' 역할을 했고, 아볼로는 그 위에 가르침을 더하여 '물을 주는' 역할을 했음을 말합니다. 그러나 진정한 변화와 성장은 오직 하나님께서 주관하신다는 점을 분명히 합니다. 이는 인간의 노력보다 하나님의 능력이 우선함을 보여주며, 교회의 일꾼들은 각자 맡은 역할에 충실할 뿐 결과는 하나님께 맡겨야 함을 가르칩니다.

### verseId 28418

- summary: 바울은 사역자들은 단지 도구일 뿐이며, 진정한 성장의 근원은 하나님이심을 강조합니다.
- explanation: 이 구절은 고린도 교회의 분열 문제를 다루는 문맥에서 나옵니다. 바울은 자신과 아볼로 같은 사역자들이 심고 물을 주는 역할을 하지만, 실제로 자라게 하시는 분은 오직 하나님이라고 설명합니다. 이는 모든 사역의 궁극적인 주인이 하나님이시며, 인간 사역자는 그분의 도구에 불과하다는 교훈을 줍니다.

### verseId 28419

- summary: 심는 사람과 물 주는 사람은 같은 목적을 위해 함께 일하며, 각자 자신의 수고에 따라 상을 받게 됩니다.
- explanation: 이 구절은 하나님의 일에 참여하는 모든 사람이 각자 맡은 역할이 다를지라도 모두 동등하게 중요하며, 각자의 노력과 충성에 따라 공정한 보상을 받을 것임을 가르칩니다. 이는 협력과 개인의 책임을 강조하는 말씀입니다.

### verseId 28420

- summary: 바울은 자신과 동역자들이 하나님과 함께 일하는 일꾼이며, 고린도 성도들은 하나님이 가꾸시는 밭이자 세우시는 집이라고 설명합니다.
- explanation: 이 구절에서 바울은 그리스도인 공동체의 정체성을 세 가지 비유로 보여줍니다. 첫째, '하나님의 동역자'는 사역자들이 하나님의 뜻을 따라 함께 수고하는 존재임을 나타냅니다. 둘째, '하나님의 밭'은 성도들이 하나님이 심고 기르시는 농작물과 같아서, 그분의 돌봄과 성장을 필요로 함을 의미합니다. 셋째, '하나님의 집'은 성도들이 하나님이 거하시는 건물처럼 서로 연결되어 세워져 가는 공동체임을 강조합니다. 이 모든 비유는 인간의 노력보다 하나님의 주권적인 역사가 중심에 있음을 부드럽게 알려줍니다.

### verseId 28421

- summary: 바울은 자신이 하나님의 은혜로 지혜로운 건축자처럼 기초를 놓았고, 다른 사람들이 그 위에 세우고 있음을 말합니다. 그리고 각 사람이 어떻게 세울지 조심해야 한다고 경고합니다.
- explanation: 이 구절에서 바울은 자신을 '지혜로운 건축자'에 비유하며, 그가 전한 복음이 교회의 기초임을 설명합니다. '하나님의 은혜'는 바울이 사도로서 받은 소명과 능력을 가리킵니다. '다른 이가 그 위에 세우나'는 바울 이후에 교회를 가르치고 세우는 다른 지도자들을 의미합니다. 바울은 모든 사람이 이 기초 위에 무엇을 세울지 신중해야 한다고 강조하는데, 이는 교회의 가르침과 삶이 복음의 기초에 충실해야 함을 경고하는 것입니다.

### verseId 28422

- summary: 오직 예수 그리스도만이 우리 신앙의 유일한 기초입니다.
- explanation: 이 구절은 신앙의 기초가 오직 예수 그리스도 한 분뿐임을 강조합니다. 다른 어떤 사람이나 가르침, 철학도 이 기초를 대신할 수 없습니다. 우리의 구원과 신앙 생활은 오직 그리스도의 십자가와 부활 위에 세워져야 합니다.

### verseId 28423

- summary: 바울은 예수 그리스도라는 기초 위에 세워지는 다양한 재료들을 비유로 설명합니다. 금, 은, 보석은 영원한 가치가 있는 신실한 삶과 가르침을, 나무, 풀, 짚은 불완전하고 일시적인 것들을 상징합니다.
- explanation: 이 구절은 고린도전서 3장에서 바울이 교회의 기초이신 예수 그리스도 위에 각자 어떻게 세워가는지를 설명하는 부분입니다. '금, 은, 보석'은 하나님의 말씀에 순종하고 영원한 가치가 있는 일들을 의미하며, '나무, 풀, 짚'은 세상적인 방법이나 잘못된 가르침, 혹은 공허한 업적을 나타냅니다. 바울은 우리가 어떤 재료로 인생을 건축하는지가 심판 날에 드러날 것이라고 경고합니다.

### verseId 28424

- summary: 하나님 앞에서 우리의 삶의 일들이 평가받을 날이 옵니다. 그 평가는 불과 같은 시험을 통해 각 사람이 쌓아온 것이 진정한 가치가 있는지 드러나게 됩니다.
- explanation: 이 구절은 그리스도인들이 이 땅에서 행한 모든 일이 장차 하나님의 심판대 앞에서 평가될 것임을 가르칩니다. '그 날'은 예수 그리스도의 재림과 함께 오는 심판의 날을 의미합니다. '불'은 하나님의 거룩한 심판과 시험을 상징하며, 이 불 앞에서 각 사람의 일이 어떤 재료로 지어졌는지(금, 은, 보석인지, 나무, 풀, 짚인지) 드러나게 됩니다. 이는 우리의 구원이 아니라, 구원받은 자의 삶의 열매와 사역의 질이 평가되는 것입니다.

### verseId 28425

- summary: 하나님의 일을 충성되이 행한 사람은 그 수고에 대한 상을 받게 됩니다.
- explanation: 이 구절은 신자가 예수 그리스도의 기초 위에 세운 삶의 공력(일)이 심판 날에 불로 시험될 때 견디어 남으면, 그 수고에 대한 상급을 받게 될 것이라고 말합니다. 이는 구원이 행위로 말미암는 것이 아니라, 구원받은 자의 삶이 하나님 앞에 열매로 인정받고 보상받는다는 원리를 보여줍니다.

### verseId 28426

- summary: 이 구절은 신자가 예수 그리스도의 기초 위에 세우는 삶의 공력(일)이 심판 날에 불로 시험될 때, 그 공력이 불타 없어지면 손해를 보지만 그 사람 자신은 구원을 얻는다고 말합니다. 이는 구원이 행위가 아니라 믿음으로 말미암는 은혜임을 강조합니다.
- explanation: 고린도전서 3장 15절은 그리스도인의 삶의 질과 그에 따른 상급에 대해 가르칩니다. '공력'은 신앙과 삶의 열매를 상징하며, '불'은 하나님의 심판과 시험을 의미합니다. 만약 신자가 세운 일이 불타 없어질 정도로 가치가 없다면, 그는 그 일에 대한 상급을 잃지만 자신의 구원은 잃지 않습니다. 구원은 오직 예수 그리스도를 믿음으로 얻는 선물이기 때문입니다. '불 가운데서 얻은 것 같으리라'는 표현은 구원이 간신히 이루어지는 듯한 위기 상황을 비유하지만, 실제로는 구원의 확실성을 강조합니다.

## 검증

| 구분 | 결과 |
| --- | --- |
| full manual test | `BUILD SUCCESSFUL` |
| 회귀/경계 테스트 | `BUILD SUCCESSFUL` |
| asset 상태 DB 확인 | asset 1-15 모두 `APPROVED` |
| 고객 노출 DB 확인 | verse 28412-28426 모두 `APPROVED + ACTIVE` |
| validation log DB 확인 | asset 1-15 모두 layer 1/2 `PASSED` |
| checklist 저장 경계 확인 | 민감 키 매치 `0` |
| ignored 산출물 확인 | `restricted/**`, `build/**`, PDF는 stage 대상 제외 |

## 비고

- report에는 고객 노출 테이블에 저장된 `summary`와 `explanation`만 기록했다.
- prompt 전문, 제공자 응답 전문, 참조자료 원문, DeepSeek 비밀값은 기록하지 않았다.
- `qtai-server/build/ai-review-reference/bible-verse-customer-exposure-full-summary.json`은 재현/점검용 로컬 산출물이며 Git에 포함하지 않는다.
