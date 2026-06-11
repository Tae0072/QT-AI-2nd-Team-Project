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
  String get loginWebNotSupported => '웹에서는 카카오 로그인을 지원하지 않습니다. 모바일 앱을 이용해 주세요.';

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
  String get notePublishTooltip => '나눔 공개';

  @override
  String get notePublishSheetTitle => '닉네임 나눔에 공개';

  @override
  String get notePublishNicknameNotice => '공개하면 내 닉네임이 함께 표시됩니다.';

  @override
  String get notePublishCommentsLabel => '댓글 허용';

  @override
  String get notePublishConfirm => '공개';

  @override
  String get notePublishNeedSave => '저장을 완료한 노트만 공개할 수 있어요';

  @override
  String get notePublishSuccess => '나눔에 공개되었습니다';

  @override
  String get notePublishFailed => '나눔 공개에 실패했습니다. 다시 시도해 주세요';

  @override
  String get notePublishAlready => '이미 나눔에 공개된 노트예요';

  @override
  String get notePublishView => '보기';

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
  String get calDayChecklistTitle => '이 날 작성한 노트';

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
  String get navToday => 'QT';

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
  String get bibleSimulator => '애니메이션';

  @override
  String get qtVideoTitle => 'QT 영상';

  @override
  String get qtVideoRetry => '다시 불러오기';

  @override
  String get videoBack => '뒤로';

  @override
  String get videoPlay => '재생';

  @override
  String get videoPause => '일시정지';

  @override
  String get videoSpeed => '배속';

  @override
  String get videoFullscreen => '전체화면';

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

  @override
  String get sharingMine => '내 나눔';

  @override
  String get sharingSearchHint => '나눔 글 검색';

  @override
  String get sharingFeedEmpty => '나눔 글이 없습니다';

  @override
  String get sharingMineEmpty => '공유한 글이 없습니다';

  @override
  String get sharingHidden => '숨김';

  @override
  String get sharingShow => '공개로 되돌리기';

  @override
  String get sharingHide => '숨기기';

  @override
  String get sharingActionFailed => '처리에 실패했습니다. 다시 시도해 주세요';

  @override
  String get sharingDeleteConfirmTitle => '나눔 글을 삭제할까요?';

  @override
  String get sharingDeleteConfirmBody => '삭제하면 되돌릴 수 없습니다.';

  @override
  String get sharingCommentFailed => '댓글 작성에 실패했습니다';

  @override
  String get sharingCommentDeleteFailed => '댓글 삭제에 실패했습니다';

  @override
  String get reportSpam => '스팸/광고';

  @override
  String get reportHate => '혐오/욕설';

  @override
  String get reportSexual => '선정성';

  @override
  String get reportEtc => '기타';

  @override
  String get sharingReportPrompt => '신고 사유를 선택하세요';

  @override
  String get sharingReportSubmitted => '신고가 접수되었습니다';

  @override
  String get sharingReportFailed => '신고에 실패했습니다';

  @override
  String get sharingUnlikeFailed => '좋아요 취소에 실패했습니다';

  @override
  String get sharingLikeFailed => '좋아요에 실패했습니다';

  @override
  String get sharingDeleteTitle => '나눔 글 삭제';

  @override
  String get sharingDeleteConfirmBody2 => '삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.';

  @override
  String get sharingDeleteFailed => '삭제에 실패했습니다';

  @override
  String get sharingDetailTitle => '나눔 상세';

  @override
  String get sharingReport => '신고';

  @override
  String get sharingLoadFailed => '글을 불러올 수 없습니다';

  @override
  String get sharingComments => '댓글';

  @override
  String get sharingCommentHint => '댓글을 입력하세요';

  @override
  String get sharingNoComments => '첫 댓글을 남겨보세요';

  @override
  String get sharingCommentsDisabled => '댓글이 비활성화된 글입니다';

  @override
  String get emptyDefault => '데이터가 없습니다.';

  @override
  String get mypageTitle => '마이페이지';

  @override
  String get mypagePartialError => '일부 정보를 불러오지 못했습니다';

  @override
  String get mypageViewProfile => '프로필 보기';

  @override
  String get withdrawTitle => '회원 탈퇴';

  @override
  String get withdrawConfirm => '탈퇴';

  @override
  String get withdrawBody =>
      '탈퇴 시 계정은 비활성화되며, 개인정보와 작성 기록은 관련 법령에 따라 2년간 보관 후 자동 삭제됩니다.\n\n보관 기간 내 같은 카카오 계정으로 다시 로그인하면 계정과 기록이 복구됩니다.\n\n정말 탈퇴하시겠습니까?';

  @override
  String get qmNotifications => '알림';

  @override
  String get qmMyPraise => '나의 찬양';

  @override
  String get qmSettings => '설정';

  @override
  String qmSongCount(int count) {
    return '$count곡';
  }

  @override
  String get statsTitle => '나의 묵상';

  @override
  String get statsWeek => '이번 주';

  @override
  String get statsMonth => '이번 달';

  @override
  String get statsStreak => '연속';

  @override
  String statsDays(int days) {
    return '$days일';
  }

  @override
  String get notifMarkAllRead => '모두 읽음';

  @override
  String get notifUnreadOnly => '안 읽은 알림만';

  @override
  String get notifEmpty => '알림이 없습니다';

  @override
  String get timeJustNow => '방금 전';

  @override
  String timeMinutesAgo(int minutes) {
    return '$minutes분 전';
  }

  @override
  String timeHoursAgo(int hours) {
    return '$hours시간 전';
  }

  @override
  String timeDaysAgo(int days) {
    return '$days일 전';
  }

  @override
  String get settingsNotification => '알림 수신';

  @override
  String get settingsNotificationDesc => '푸시 알림을 받습니다';

  @override
  String get settingsFontSize => '폰트 크기';

  @override
  String get settingsFontSizeDesc => '본문 글자 크기를 설정합니다';

  @override
  String get settingsFontSmall => '작게';

  @override
  String get settingsFontMedium => '보통';

  @override
  String get settingsFontLarge => '크게';

  @override
  String get settingsTts => 'TTS 읽기 설정';

  @override
  String get settingsTtsDesc => '읽기 목소리와 읽기 범위(본문/해설)를 설정합니다';

  @override
  String get praiseTitle => '찬양';

  @override
  String get praiseMyTab => '내 찬양';

  @override
  String get praiseCurationTab => '큐레이션';

  @override
  String get praiseMyEmpty => '저장한 찬양이 없습니다\n큐레이션에서 찬양을 저장해보세요';

  @override
  String get praiseDeleted => '찬양이 삭제되었습니다';

  @override
  String get praiseCurationEmpty => '등록된 큐레이션 곡이 없습니다';

  @override
  String get praiseSaved => '내 찬양에 저장되었습니다';

  @override
  String get praiseAlreadySaved => '이미 저장된 곡입니다';

  @override
  String get profileTitle => '프로필';

  @override
  String get profileEmail => '이메일';

  @override
  String get profileJoinDate => '가입일';

  @override
  String get profileLogout => '로그아웃';

  @override
  String get profileNickname => '닉네임';

  @override
  String get profileNewNickname => '새 닉네임';

  @override
  String get profileNicknameHint2 => '2~20자';

  @override
  String get profileNicknameAvailable => '사용 가능한 닉네임입니다.';

  @override
  String get profileNicknameTaken => '이미 사용 중인 닉네임입니다.';

  @override
  String get profileChange => '변경';

  @override
  String get profileNicknameChanged => '닉네임이 변경되었습니다.';

  @override
  String get profileNicknameChangeFailed => '닉네임 변경에 실패했습니다.';

  @override
  String get profileLogoutFailed => '로그아웃에 실패했습니다.';

  @override
  String get profileWithdrawFailed => '탈퇴 처리에 실패했습니다.';

  @override
  String get ttsVoice => '읽기 목소리';

  @override
  String get ttsVoiceDesc => 'QT 본문을 읽어주는 목소리를 설정합니다';

  @override
  String get ttsReadBible => '본문 읽기 (한글)';

  @override
  String get ttsReadBibleDesc => 'QT 한글 본문을 읽어줍니다';

  @override
  String get ttsReadExplanation => '해설 읽기';

  @override
  String get ttsReadExplanationDesc => '본문 해설을 읽어줍니다. 본문 읽기와 함께 켜면 본문 후에 읽습니다';

  @override
  String get ttsAtLeastOne => '본문과 해설 중 최소 한 가지는 켜져 있어야 합니다';

  @override
  String get ttsServerError => 'TTS 서버에 연결할 수 없습니다';

  @override
  String get ttsCustomVoice => '커스텀 목소리';

  @override
  String get ttsDefaultVoice => '기본 목소리';

  @override
  String get ttsFinetuned => '학습됨';

  @override
  String get ttsVoicesLoading => '목소리 목록을 불러오는 중입니다';

  @override
  String get ttsVoicesError => '목소리 목록을 불러올 수 없습니다';

  @override
  String get catMeditation => '묵상';

  @override
  String get catSermon => '설교';

  @override
  String get catPrayer => '기도';

  @override
  String get catGratitude => '감사';

  @override
  String get catRepentance => '회개';
}
