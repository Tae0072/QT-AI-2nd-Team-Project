// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Korean (`ko`).
class AppLocalizationsKo extends AppLocalizations {
  AppLocalizationsKo([String locale = 'ko']) : super(locale);

  @override
  String get appTitle => 'QT AI';

  @override
  String get splashSubtitle => '매일, 말씀 앞에 머무는 시간';

  @override
  String get commonSave => '저장';

  @override
  String get commonCancel => '취소';

  @override
  String get commonConfirm => '확인';

  @override
  String get commonDelete => '삭제';

  @override
  String get commonEdit => '수정';

  @override
  String get commonClose => '닫기';

  @override
  String get commonRetry => '다시 시도';

  @override
  String get commonLoading => '불러오는 중...';

  @override
  String get commonBack => '뒤로';

  @override
  String get loginHeadline => '매일의 묵상을\n가장 단순하게.';

  @override
  String get loginSubtitle => '오늘의 QT 본문과 검증된 해설을 읽고,\n나만의 묵상 노트를 남겨보세요.';

  @override
  String get loginKakaoButton => '카카오로 시작하기';

  @override
  String get loginFailed => '로그인에 실패했습니다. 다시 시도해주세요.';

  @override
  String get loginLegalPrefix => '계속 진행하면 ';

  @override
  String get loginTermsOfService => '이용약관';

  @override
  String get loginLegalAnd => ' 및 ';

  @override
  String get loginPrivacyPolicy => '개인정보처리방침';

  @override
  String get loginLegalSuffix => '에\n동의하는 것으로 간주합니다.';

  @override
  String get nicknameWelcome => '반갑습니다!';

  @override
  String get nicknameSetupPrompt => '사용할 닉네임을 설정해주세요';

  @override
  String get nicknameHint => '닉네임 (2~10자)';

  @override
  String get nicknameHelper => '한글, 영문, 숫자 조합 가능 (2~10자)';

  @override
  String get nicknameStartButton => '시작하기';

  @override
  String get nicknameRequired => '닉네임을 입력해주세요';

  @override
  String get nicknameMinLength => '닉네임은 2자 이상이어야 합니다';

  @override
  String get nicknameMaxLength => '닉네임은 10자 이하여야 합니다';

  @override
  String get nicknameInvalidChars => '한글, 영문, 숫자만 사용할 수 있습니다';

  @override
  String get nicknameSetupFailed => '닉네임 설정에 실패했습니다. 다시 시도해주세요.';

  @override
  String get noteNewTitle => '새 노트';

  @override
  String get noteCategoryPrompt => '어떤 노트를 작성할까요?';

  @override
  String get noteListTitle => '노트';

  @override
  String get noteViewList => '목록 보기';

  @override
  String get noteViewCalendar => '달력 보기';

  @override
  String get noteFilterAll => '전체';

  @override
  String get noteEmpty => '작성한 노트가 없습니다';

  @override
  String get noteUntitled => '(제목 없음)';

  @override
  String get noteDraft => '임시저장';

  @override
  String get noteModeWrite => '작성';

  @override
  String get noteShareTooltip => '공유';

  @override
  String get noteShared => '공유됨';

  @override
  String get noteNoContent => '(내용 없음)';

  @override
  String get noteDeleteConfirmTitle => '노트를 삭제할까요?';

  @override
  String get noteDeleteConfirmBody => '삭제한 노트는 되돌릴 수 없습니다.';

  @override
  String get noteDeleted => '삭제되었습니다';

  @override
  String get noteDeleteFailed => '삭제에 실패했습니다. 다시 시도해 주세요';

  @override
  String get noteSectionFelt => '느낀 점';

  @override
  String get noteSectionVerse => '기억할 구절';

  @override
  String get noteSectionApply => '적용할 점';

  @override
  String get noteSectionPray => '기도';

  @override
  String get noteQuotedVerses => '인용 구절';

  @override
  String get noteLoadFailed => '노트를 불러오지 못했습니다';

  @override
  String get noteEditTitleLabel => '제목 (선택)';

  @override
  String get noteEditBodyLabel => '본문';

  @override
  String get noteEditBodyRequired => '본문을 입력해 주세요';

  @override
  String get noteEditTitleOrBodyRequired => '제목이나 본문을 입력해 주세요';

  @override
  String get noteSaved => '저장되었습니다';

  @override
  String get noteDraftSaved => '임시저장되었습니다';

  @override
  String get noteSaveFailed => '저장에 실패했습니다. 다시 시도해 주세요';

  @override
  String get fmtBold => '굵게';

  @override
  String get fmtItalic => '기울임';

  @override
  String get fmtHeading => '제목';

  @override
  String get fmtList => '목록';

  @override
  String get fmtQuote => '인용';

  @override
  String get fmtCheckbox => '체크박스';

  @override
  String get fmtDivider => '구분선';

  @override
  String calSavedThisMonth(int days) {
    return '이번 달 $days일 저장';
  }

  @override
  String calNoteCount(int count) {
    return '노트 $count개';
  }

  @override
  String calStreak(int days) {
    return '연속 $days일 🔥';
  }

  @override
  String get noteShareAsText => '텍스트로 공유';

  @override
  String get noteShareAsImage => '이미지로 공유';

  @override
  String get noteShareImageFailed => '이미지 공유에 실패했습니다';

  @override
  String get navToday => '오늘';

  @override
  String get navBible => '성경';

  @override
  String get navShare => '나눔';

  @override
  String get navNote => '노트';

  @override
  String get navMy => '마이';

  @override
  String get ttsRead => '본문 읽기';

  @override
  String get ttsStop => '읽기 정지';

  @override
  String get ttsNoExplanationInfo => '오늘 QT의 해설 정보가 없습니다';

  @override
  String get ttsNoExplanationReady => '아직 준비된 해설이 없습니다';

  @override
  String get ttsOnlyBodyNoExplanation => '해설이 아직 없어 본문만 읽습니다';

  @override
  String get ttsExplanationLoadFailed => '해설을 불러오지 못했습니다';

  @override
  String get ttsOnlyBodyExplanationFailed => '해설을 불러오지 못해 본문만 읽습니다';

  @override
  String get ttsTokenMissing => 'TTS 토큰이 설정되지 않았습니다';

  @override
  String get ttsTurnOnReadItems => '설정에서 읽을 항목(본문/주석)을 켜 주세요';

  @override
  String get ttsPrepareFailed => '음성을 준비하지 못했습니다';

  @override
  String get onbSkip => '건너뛰기';

  @override
  String get onbNext => '다음';

  @override
  String get onbStart => '시작하기';

  @override
  String get commonRefresh => '새로고침';

  @override
  String get bibleTodayQt => '오늘 QT';

  @override
  String get bibleTodayLoading => '오늘 본문을 불러오는 중입니다.';

  @override
  String get bibleTodayLoadError => '오늘 본문을 불러오지 못했습니다.';

  @override
  String get bibleExplanation => '해설';

  @override
  String get bibleSimulator => '시뮬레이터';

  @override
  String get bibleMeditationNote => '묵상 노트 작성';

  @override
  String bibleComingSoon(String feature) {
    return '$feature 화면은 곧 제공됩니다.';
  }

  @override
  String get bibleBrowserTitle => '성경본문';

  @override
  String get bibleChapterError => '장과 절은 1 이상의 숫자로 입력해 주세요.';

  @override
  String get bibleBooksLoading => '성경 권 목록을 불러오는 중입니다.';

  @override
  String get bibleBooksLoadError => '성경 권 목록을 불러오지 못했습니다.';

  @override
  String get bibleBooksEmpty => '성경 권 목록이 없습니다.';

  @override
  String get bibleChapter => '장';

  @override
  String get bibleVerseFrom => '시작절';

  @override
  String get bibleVerseTo => '끝절';

  @override
  String get bibleSame => '동일';

  @override
  String get bibleSearch => '조회';

  @override
  String get bibleVersesLoadError => '성경본문을 불러오지 못했습니다.';

  @override
  String get bibleSelectPrompt => '조회할 성경본문을 선택해 주세요.';
}
