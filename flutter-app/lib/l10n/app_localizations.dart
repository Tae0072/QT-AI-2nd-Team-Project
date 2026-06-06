import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_ko.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
      : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('ko')
  ];

  /// App name shown in the title bar
  ///
  /// In en, this message translates to:
  /// **'QT AI'**
  String get appTitle;

  /// Subtitle shown on the splash screen
  ///
  /// In en, this message translates to:
  /// **'Time to dwell on the Word, every day'**
  String get splashSubtitle;

  /// No description provided for @commonSave.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get commonSave;

  /// No description provided for @commonCancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get commonCancel;

  /// No description provided for @commonConfirm.
  ///
  /// In en, this message translates to:
  /// **'Confirm'**
  String get commonConfirm;

  /// No description provided for @commonDelete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get commonDelete;

  /// No description provided for @commonEdit.
  ///
  /// In en, this message translates to:
  /// **'Edit'**
  String get commonEdit;

  /// No description provided for @commonClose.
  ///
  /// In en, this message translates to:
  /// **'Close'**
  String get commonClose;

  /// No description provided for @commonRetry.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get commonRetry;

  /// No description provided for @commonLoading.
  ///
  /// In en, this message translates to:
  /// **'Loading...'**
  String get commonLoading;

  /// No description provided for @commonBack.
  ///
  /// In en, this message translates to:
  /// **'Back'**
  String get commonBack;

  /// No description provided for @loginHeadline.
  ///
  /// In en, this message translates to:
  /// **'Daily meditation, made simplest.'**
  String get loginHeadline;

  /// No description provided for @loginSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Read today\'s QT passage and verified commentary, and leave your own meditation notes.'**
  String get loginSubtitle;

  /// No description provided for @loginKakaoButton.
  ///
  /// In en, this message translates to:
  /// **'Start with Kakao'**
  String get loginKakaoButton;

  /// No description provided for @loginFailed.
  ///
  /// In en, this message translates to:
  /// **'Login failed. Please try again.'**
  String get loginFailed;

  /// No description provided for @loginLegalPrefix.
  ///
  /// In en, this message translates to:
  /// **'By continuing, you agree to the '**
  String get loginLegalPrefix;

  /// No description provided for @loginTermsOfService.
  ///
  /// In en, this message translates to:
  /// **'Terms of Service'**
  String get loginTermsOfService;

  /// No description provided for @loginLegalAnd.
  ///
  /// In en, this message translates to:
  /// **' and '**
  String get loginLegalAnd;

  /// No description provided for @loginPrivacyPolicy.
  ///
  /// In en, this message translates to:
  /// **'Privacy Policy'**
  String get loginPrivacyPolicy;

  /// No description provided for @loginLegalSuffix.
  ///
  /// In en, this message translates to:
  /// **'.'**
  String get loginLegalSuffix;

  /// No description provided for @nicknameWelcome.
  ///
  /// In en, this message translates to:
  /// **'Welcome!'**
  String get nicknameWelcome;

  /// No description provided for @nicknameSetupPrompt.
  ///
  /// In en, this message translates to:
  /// **'Please set a nickname to use'**
  String get nicknameSetupPrompt;

  /// No description provided for @nicknameHint.
  ///
  /// In en, this message translates to:
  /// **'Nickname (2-10 chars)'**
  String get nicknameHint;

  /// No description provided for @nicknameHelper.
  ///
  /// In en, this message translates to:
  /// **'Korean, English, and numbers allowed (2-10 chars)'**
  String get nicknameHelper;

  /// No description provided for @nicknameStartButton.
  ///
  /// In en, this message translates to:
  /// **'Get started'**
  String get nicknameStartButton;

  /// No description provided for @nicknameRequired.
  ///
  /// In en, this message translates to:
  /// **'Please enter a nickname'**
  String get nicknameRequired;

  /// No description provided for @nicknameMinLength.
  ///
  /// In en, this message translates to:
  /// **'Nickname must be at least 2 characters'**
  String get nicknameMinLength;

  /// No description provided for @nicknameMaxLength.
  ///
  /// In en, this message translates to:
  /// **'Nickname must be 10 characters or fewer'**
  String get nicknameMaxLength;

  /// No description provided for @nicknameInvalidChars.
  ///
  /// In en, this message translates to:
  /// **'Only Korean, English, and numbers are allowed'**
  String get nicknameInvalidChars;

  /// No description provided for @nicknameSetupFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to set nickname. Please try again.'**
  String get nicknameSetupFailed;

  /// No description provided for @noteNewTitle.
  ///
  /// In en, this message translates to:
  /// **'New note'**
  String get noteNewTitle;

  /// No description provided for @noteCategoryPrompt.
  ///
  /// In en, this message translates to:
  /// **'What kind of note?'**
  String get noteCategoryPrompt;

  /// No description provided for @noteListTitle.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get noteListTitle;

  /// No description provided for @noteViewList.
  ///
  /// In en, this message translates to:
  /// **'List view'**
  String get noteViewList;

  /// No description provided for @noteViewCalendar.
  ///
  /// In en, this message translates to:
  /// **'Calendar view'**
  String get noteViewCalendar;

  /// No description provided for @noteFilterAll.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get noteFilterAll;

  /// No description provided for @noteEmpty.
  ///
  /// In en, this message translates to:
  /// **'No notes yet'**
  String get noteEmpty;

  /// No description provided for @noteUntitled.
  ///
  /// In en, this message translates to:
  /// **'(Untitled)'**
  String get noteUntitled;

  /// No description provided for @noteDraft.
  ///
  /// In en, this message translates to:
  /// **'Draft'**
  String get noteDraft;

  /// No description provided for @noteModeWrite.
  ///
  /// In en, this message translates to:
  /// **'New'**
  String get noteModeWrite;

  /// No description provided for @noteShareTooltip.
  ///
  /// In en, this message translates to:
  /// **'Share'**
  String get noteShareTooltip;

  /// No description provided for @noteShared.
  ///
  /// In en, this message translates to:
  /// **'Shared'**
  String get noteShared;

  /// No description provided for @noteNoContent.
  ///
  /// In en, this message translates to:
  /// **'(No content)'**
  String get noteNoContent;

  /// No description provided for @noteDeleteConfirmTitle.
  ///
  /// In en, this message translates to:
  /// **'Delete this note?'**
  String get noteDeleteConfirmTitle;

  /// No description provided for @noteDeleteConfirmBody.
  ///
  /// In en, this message translates to:
  /// **'Deleted notes cannot be recovered.'**
  String get noteDeleteConfirmBody;

  /// No description provided for @noteDeleted.
  ///
  /// In en, this message translates to:
  /// **'Deleted'**
  String get noteDeleted;

  /// No description provided for @noteDeleteFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to delete. Please try again.'**
  String get noteDeleteFailed;

  /// No description provided for @noteSectionFelt.
  ///
  /// In en, this message translates to:
  /// **'Reflections'**
  String get noteSectionFelt;

  /// No description provided for @noteSectionVerse.
  ///
  /// In en, this message translates to:
  /// **'Verse to remember'**
  String get noteSectionVerse;

  /// No description provided for @noteSectionApply.
  ///
  /// In en, this message translates to:
  /// **'How to apply'**
  String get noteSectionApply;

  /// No description provided for @noteSectionPray.
  ///
  /// In en, this message translates to:
  /// **'Prayer'**
  String get noteSectionPray;

  /// No description provided for @noteQuotedVerses.
  ///
  /// In en, this message translates to:
  /// **'Quoted verses'**
  String get noteQuotedVerses;

  /// No description provided for @noteLoadFailed.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load the note'**
  String get noteLoadFailed;

  /// No description provided for @noteEditTitleLabel.
  ///
  /// In en, this message translates to:
  /// **'Title (optional)'**
  String get noteEditTitleLabel;

  /// No description provided for @noteEditBodyLabel.
  ///
  /// In en, this message translates to:
  /// **'Content'**
  String get noteEditBodyLabel;

  /// No description provided for @noteEditBodyRequired.
  ///
  /// In en, this message translates to:
  /// **'Please enter the content'**
  String get noteEditBodyRequired;

  /// No description provided for @noteEditTitleOrBodyRequired.
  ///
  /// In en, this message translates to:
  /// **'Please enter a title or content'**
  String get noteEditTitleOrBodyRequired;

  /// No description provided for @noteSaved.
  ///
  /// In en, this message translates to:
  /// **'Saved'**
  String get noteSaved;

  /// No description provided for @noteDraftSaved.
  ///
  /// In en, this message translates to:
  /// **'Saved as draft'**
  String get noteDraftSaved;

  /// No description provided for @noteSaveFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to save. Please try again.'**
  String get noteSaveFailed;

  /// No description provided for @fmtBold.
  ///
  /// In en, this message translates to:
  /// **'Bold'**
  String get fmtBold;

  /// No description provided for @fmtItalic.
  ///
  /// In en, this message translates to:
  /// **'Italic'**
  String get fmtItalic;

  /// No description provided for @fmtHeading.
  ///
  /// In en, this message translates to:
  /// **'Heading'**
  String get fmtHeading;

  /// No description provided for @fmtList.
  ///
  /// In en, this message translates to:
  /// **'List'**
  String get fmtList;

  /// No description provided for @fmtQuote.
  ///
  /// In en, this message translates to:
  /// **'Quote'**
  String get fmtQuote;

  /// No description provided for @fmtCheckbox.
  ///
  /// In en, this message translates to:
  /// **'Checkbox'**
  String get fmtCheckbox;

  /// No description provided for @fmtDivider.
  ///
  /// In en, this message translates to:
  /// **'Divider'**
  String get fmtDivider;

  /// No description provided for @calSavedThisMonth.
  ///
  /// In en, this message translates to:
  /// **'Saved {days} days this month'**
  String calSavedThisMonth(int days);

  /// No description provided for @calNoteCount.
  ///
  /// In en, this message translates to:
  /// **'{count} notes'**
  String calNoteCount(int count);

  /// No description provided for @calStreak.
  ///
  /// In en, this message translates to:
  /// **'{days}-day streak 🔥'**
  String calStreak(int days);

  /// No description provided for @noteShareAsText.
  ///
  /// In en, this message translates to:
  /// **'Share as text'**
  String get noteShareAsText;

  /// No description provided for @noteShareAsImage.
  ///
  /// In en, this message translates to:
  /// **'Share as image'**
  String get noteShareAsImage;

  /// No description provided for @noteShareImageFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to share image'**
  String get noteShareImageFailed;

  /// No description provided for @navToday.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get navToday;

  /// No description provided for @navBible.
  ///
  /// In en, this message translates to:
  /// **'Bible'**
  String get navBible;

  /// No description provided for @navShare.
  ///
  /// In en, this message translates to:
  /// **'Sharing'**
  String get navShare;

  /// No description provided for @navNote.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get navNote;

  /// No description provided for @navMy.
  ///
  /// In en, this message translates to:
  /// **'My'**
  String get navMy;

  /// No description provided for @ttsRead.
  ///
  /// In en, this message translates to:
  /// **'Read passage'**
  String get ttsRead;

  /// No description provided for @ttsStop.
  ///
  /// In en, this message translates to:
  /// **'Stop reading'**
  String get ttsStop;

  /// No description provided for @ttsNoExplanationInfo.
  ///
  /// In en, this message translates to:
  /// **'No commentary info for today\'s QT'**
  String get ttsNoExplanationInfo;

  /// No description provided for @ttsNoExplanationReady.
  ///
  /// In en, this message translates to:
  /// **'No commentary is ready yet'**
  String get ttsNoExplanationReady;

  /// No description provided for @ttsOnlyBodyNoExplanation.
  ///
  /// In en, this message translates to:
  /// **'No commentary yet, reading the passage only'**
  String get ttsOnlyBodyNoExplanation;

  /// No description provided for @ttsExplanationLoadFailed.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load the commentary'**
  String get ttsExplanationLoadFailed;

  /// No description provided for @ttsOnlyBodyExplanationFailed.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load commentary, reading the passage only'**
  String get ttsOnlyBodyExplanationFailed;

  /// No description provided for @ttsTokenMissing.
  ///
  /// In en, this message translates to:
  /// **'TTS token is not set'**
  String get ttsTokenMissing;

  /// No description provided for @ttsTurnOnReadItems.
  ///
  /// In en, this message translates to:
  /// **'Turn on items to read (passage/commentary) in settings'**
  String get ttsTurnOnReadItems;

  /// No description provided for @ttsPrepareFailed.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t prepare the audio'**
  String get ttsPrepareFailed;

  /// No description provided for @onbSkip.
  ///
  /// In en, this message translates to:
  /// **'Skip'**
  String get onbSkip;

  /// No description provided for @onbNext.
  ///
  /// In en, this message translates to:
  /// **'Next'**
  String get onbNext;

  /// No description provided for @onbStart.
  ///
  /// In en, this message translates to:
  /// **'Get started'**
  String get onbStart;

  /// No description provided for @commonRefresh.
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get commonRefresh;

  /// No description provided for @bibleTodayQt.
  ///
  /// In en, this message translates to:
  /// **'Today\'s QT'**
  String get bibleTodayQt;

  /// No description provided for @bibleTodayLoading.
  ///
  /// In en, this message translates to:
  /// **'Loading today\'s passage...'**
  String get bibleTodayLoading;

  /// No description provided for @bibleTodayLoadError.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load today\'s passage.'**
  String get bibleTodayLoadError;

  /// No description provided for @bibleExplanation.
  ///
  /// In en, this message translates to:
  /// **'Commentary'**
  String get bibleExplanation;

  /// No description provided for @bibleSimulator.
  ///
  /// In en, this message translates to:
  /// **'Simulator'**
  String get bibleSimulator;

  /// No description provided for @bibleMeditationNote.
  ///
  /// In en, this message translates to:
  /// **'Write meditation note'**
  String get bibleMeditationNote;

  /// No description provided for @bibleComingSoon.
  ///
  /// In en, this message translates to:
  /// **'The {feature} screen is coming soon.'**
  String bibleComingSoon(String feature);

  /// No description provided for @bibleBrowserTitle.
  ///
  /// In en, this message translates to:
  /// **'Bible'**
  String get bibleBrowserTitle;

  /// No description provided for @bibleChapterError.
  ///
  /// In en, this message translates to:
  /// **'Chapter and verse must be numbers of 1 or more.'**
  String get bibleChapterError;

  /// No description provided for @bibleBooksLoading.
  ///
  /// In en, this message translates to:
  /// **'Loading the list of books...'**
  String get bibleBooksLoading;

  /// No description provided for @bibleBooksLoadError.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load the list of books.'**
  String get bibleBooksLoadError;

  /// No description provided for @bibleBooksEmpty.
  ///
  /// In en, this message translates to:
  /// **'No books available.'**
  String get bibleBooksEmpty;

  /// No description provided for @bibleChapter.
  ///
  /// In en, this message translates to:
  /// **'Chapter'**
  String get bibleChapter;

  /// No description provided for @bibleVerseFrom.
  ///
  /// In en, this message translates to:
  /// **'From verse'**
  String get bibleVerseFrom;

  /// No description provided for @bibleVerseTo.
  ///
  /// In en, this message translates to:
  /// **'To verse'**
  String get bibleVerseTo;

  /// No description provided for @bibleSame.
  ///
  /// In en, this message translates to:
  /// **'Same'**
  String get bibleSame;

  /// No description provided for @bibleSearch.
  ///
  /// In en, this message translates to:
  /// **'Search'**
  String get bibleSearch;

  /// No description provided for @bibleVersesLoadError.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load the passage.'**
  String get bibleVersesLoadError;

  /// No description provided for @bibleSelectPrompt.
  ///
  /// In en, this message translates to:
  /// **'Select a passage to look up.'**
  String get bibleSelectPrompt;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'ko'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'ko':
      return AppLocalizationsKo();
  }

  throw FlutterError(
      'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
      'an issue with the localizations generation tool. Please file an issue '
      'on GitHub with a reproducible sample app and the gen-l10n configuration '
      'that was used.');
}
