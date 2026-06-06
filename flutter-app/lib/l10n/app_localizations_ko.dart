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
}
