import 'package:flutter/foundation.dart';

import '../models/bible_models.dart';

/// ⚠️ 시연(데모) 전용 — 운영 배포 전 제거 대상.
///
/// 목적: 오늘 QT에 준비된 시뮬레이터 클립이 없을 때도 **디버그 빌드에서** QT 영상이
/// 항상 재생되게 한다(시연 녹화용). 운영(릴리스)에는 영향이 없다.
/// 안전장치:
///   1) [kDebugMode] 가 아닐 때(릴리스)는 분기 자체가 컴파일에서 제거된다.
///   2) 서버가 **이미 READY 클립을 주면 그대로 사용**하고, 없을 때만 폴백으로 대체한다.
///   3) 폴백 URL은 프로젝트 실제 시뮬레이터 영상(고린도전서 풀영상, 김태혁 제작 ·
///      GitHub 릴리스 호스팅)이다. 06-08 매핑/06-12 공개캐시/06-16 영상관리 문서 기준.
const String kDemoQtVideoUrl =
    'https://github.com/xogurrh012/qtai-bible-videos/releases/download/1co-v1/corinthians_full.mp4';

/// 디버그 + 실제 클립이 준비 안 됨일 때만 샘플 READY 클립으로 대체한다.
/// 그 외(릴리스, 이미 READY)에는 [real] 을 그대로 돌려준다.
QtVideoClip withDemoQtVideo(QtVideoClip real, int qtPassageId) {
  if (!kDebugMode || real.isReady) return real; // 릴리스/실데이터는 그대로.
  return QtVideoClip(
    status: 'READY',
    clipId: null,
    qtPassageId: qtPassageId,
    title: '[데모] 고린도전서 시뮬레이터 영상',
    videoUrl: kDemoQtVideoUrl,
    sourceVideoId: null,
    startTimeSec: null,
    endTimeSec: null,
    compositionType: null,
    clipStatus: 'READY',
  );
}

/// 디버그 + (qtPassageId 있음 + 아직 READY 아님)일 때만 시뮬레이터 상태를 READY로 바꿔
/// '영상 보기' 버튼을 켠다(시연용). 버튼을 누르면 [withDemoQtVideo]가 샘플 영상을 재생한다.
/// 그 외(릴리스, qtPassageId 없음, 이미 READY)에는 [real] 을 그대로 돌려준다.
TodayQtPassage withDemoSimulatorReady(TodayQtPassage real) {
  if (!kDebugMode ||
      real.qtPassageId == null ||
      real.simulatorStatus == 'READY') {
    return real;
  }
  return TodayQtPassage(
    qtPassageId: real.qtPassageId,
    passageDate: real.passageDate,
    title: real.title,
    cacheStatus: real.cacheStatus,
    simulatorStatus: 'READY',
    hasExplanation: real.hasExplanation,
    draftNoteId: real.draftNoteId,
    reference: real.reference,
    book: real.book,
    verses: real.verses,
  );
}