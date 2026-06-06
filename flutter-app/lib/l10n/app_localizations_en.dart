// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'QT AI';

  @override
  String get splashSubtitle => 'Time to dwell on the Word, every day';

  @override
  String get commonSave => 'Save';

  @override
  String get commonCancel => 'Cancel';

  @override
  String get commonConfirm => 'Confirm';

  @override
  String get commonDelete => 'Delete';

  @override
  String get commonEdit => 'Edit';

  @override
  String get commonClose => 'Close';

  @override
  String get commonRetry => 'Retry';

  @override
  String get commonLoading => 'Loading...';

  @override
  String get loginHeadline => 'Daily meditation, made simplest.';

  @override
  String get loginSubtitle =>
      'Read today\'s QT passage and verified commentary, and leave your own meditation notes.';

  @override
  String get loginKakaoButton => 'Start with Kakao';

  @override
  String get loginFailed => 'Login failed. Please try again.';

  @override
  String get loginLegalPrefix => 'By continuing, you agree to the ';

  @override
  String get loginTermsOfService => 'Terms of Service';

  @override
  String get loginLegalAnd => ' and ';

  @override
  String get loginPrivacyPolicy => 'Privacy Policy';

  @override
  String get loginLegalSuffix => '.';

  @override
  String get nicknameWelcome => 'Welcome!';

  @override
  String get nicknameSetupPrompt => 'Please set a nickname to use';

  @override
  String get nicknameHint => 'Nickname (2-10 chars)';

  @override
  String get nicknameHelper =>
      'Korean, English, and numbers allowed (2-10 chars)';

  @override
  String get nicknameStartButton => 'Get started';

  @override
  String get nicknameRequired => 'Please enter a nickname';

  @override
  String get nicknameMinLength => 'Nickname must be at least 2 characters';

  @override
  String get nicknameMaxLength => 'Nickname must be 10 characters or fewer';

  @override
  String get nicknameInvalidChars =>
      'Only Korean, English, and numbers are allowed';

  @override
  String get nicknameSetupFailed => 'Failed to set nickname. Please try again.';

  @override
  String get noteNewTitle => 'New note';

  @override
  String get noteCategoryPrompt => 'What kind of note?';

  @override
  String get noteListTitle => 'Notes';

  @override
  String get noteViewList => 'List view';

  @override
  String get noteViewCalendar => 'Calendar view';

  @override
  String get noteFilterAll => 'All';

  @override
  String get noteEmpty => 'No notes yet';

  @override
  String get noteUntitled => '(Untitled)';

  @override
  String get noteDraft => 'Draft';
}
