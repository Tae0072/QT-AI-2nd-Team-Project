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

  /// No description provided for @notePublishTooltip.
  ///
  /// In en, this message translates to:
  /// **'Share to feed'**
  String get notePublishTooltip;

  /// No description provided for @notePublishSheetTitle.
  ///
  /// In en, this message translates to:
  /// **'Share to nickname feed'**
  String get notePublishSheetTitle;

  /// No description provided for @notePublishNicknameNotice.
  ///
  /// In en, this message translates to:
  /// **'Your nickname will be shown when you share.'**
  String get notePublishNicknameNotice;

  /// No description provided for @notePublishCommentsLabel.
  ///
  /// In en, this message translates to:
  /// **'Allow comments'**
  String get notePublishCommentsLabel;

  /// No description provided for @notePublishConfirm.
  ///
  /// In en, this message translates to:
  /// **'Share'**
  String get notePublishConfirm;

  /// No description provided for @notePublishNeedSave.
  ///
  /// In en, this message translates to:
  /// **'Only saved notes can be shared'**
  String get notePublishNeedSave;

  /// No description provided for @notePublishSuccess.
  ///
  /// In en, this message translates to:
  /// **'Shared to the feed'**
  String get notePublishSuccess;

  /// No description provided for @notePublishFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to share. Please try again'**
  String get notePublishFailed;

  /// No description provided for @notePublishAlready.
  ///
  /// In en, this message translates to:
  /// **'This note is already shared'**
  String get notePublishAlready;

  /// No description provided for @notePublishView.
  ///
  /// In en, this message translates to:
  /// **'View'**
  String get notePublishView;

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

  /// No description provided for @calDayChecklistTitle.
  ///
  /// In en, this message translates to:
  /// **'Notes written this day'**
  String get calDayChecklistTitle;

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
  /// **'QT'**
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
  /// **'Animation'**
  String get bibleSimulator;

  /// No description provided for @qtVideoTitle.
  ///
  /// In en, this message translates to:
  /// **'QT Video'**
  String get qtVideoTitle;

  /// No description provided for @qtVideoRetry.
  ///
  /// In en, this message translates to:
  /// **'Reload'**
  String get qtVideoRetry;

  /// No description provided for @videoBack.
  ///
  /// In en, this message translates to:
  /// **'Back'**
  String get videoBack;

  /// No description provided for @videoPlay.
  ///
  /// In en, this message translates to:
  /// **'Play'**
  String get videoPlay;

  /// No description provided for @videoPause.
  ///
  /// In en, this message translates to:
  /// **'Pause'**
  String get videoPause;

  /// No description provided for @videoSpeed.
  ///
  /// In en, this message translates to:
  /// **'Speed'**
  String get videoSpeed;

  /// No description provided for @videoFullscreen.
  ///
  /// In en, this message translates to:
  /// **'Fullscreen'**
  String get videoFullscreen;

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

  /// No description provided for @sharingMine.
  ///
  /// In en, this message translates to:
  /// **'My sharing'**
  String get sharingMine;

  /// No description provided for @sharingSearchHint.
  ///
  /// In en, this message translates to:
  /// **'Search posts'**
  String get sharingSearchHint;

  /// No description provided for @sharingFeedEmpty.
  ///
  /// In en, this message translates to:
  /// **'No posts yet'**
  String get sharingFeedEmpty;

  /// No description provided for @sharingMineEmpty.
  ///
  /// In en, this message translates to:
  /// **'You haven\'t shared anything'**
  String get sharingMineEmpty;

  /// No description provided for @sharingHidden.
  ///
  /// In en, this message translates to:
  /// **'Hidden'**
  String get sharingHidden;

  /// No description provided for @sharingShow.
  ///
  /// In en, this message translates to:
  /// **'Make public again'**
  String get sharingShow;

  /// No description provided for @sharingHide.
  ///
  /// In en, this message translates to:
  /// **'Hide'**
  String get sharingHide;

  /// No description provided for @sharingActionFailed.
  ///
  /// In en, this message translates to:
  /// **'Action failed. Please try again.'**
  String get sharingActionFailed;

  /// No description provided for @sharingDeleteConfirmTitle.
  ///
  /// In en, this message translates to:
  /// **'Delete this post?'**
  String get sharingDeleteConfirmTitle;

  /// No description provided for @sharingDeleteConfirmBody.
  ///
  /// In en, this message translates to:
  /// **'This can\'t be undone.'**
  String get sharingDeleteConfirmBody;

  /// No description provided for @sharingCommentFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to post comment'**
  String get sharingCommentFailed;

  /// No description provided for @sharingCommentDeleteFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to delete comment'**
  String get sharingCommentDeleteFailed;

  /// No description provided for @reportSpam.
  ///
  /// In en, this message translates to:
  /// **'Spam/Ads'**
  String get reportSpam;

  /// No description provided for @reportHate.
  ///
  /// In en, this message translates to:
  /// **'Hate/Abuse'**
  String get reportHate;

  /// No description provided for @reportSexual.
  ///
  /// In en, this message translates to:
  /// **'Sexual content'**
  String get reportSexual;

  /// No description provided for @reportEtc.
  ///
  /// In en, this message translates to:
  /// **'Other'**
  String get reportEtc;

  /// No description provided for @sharingReportPrompt.
  ///
  /// In en, this message translates to:
  /// **'Select a report reason'**
  String get sharingReportPrompt;

  /// No description provided for @sharingReportSubmitted.
  ///
  /// In en, this message translates to:
  /// **'Report submitted'**
  String get sharingReportSubmitted;

  /// No description provided for @sharingReportFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to report'**
  String get sharingReportFailed;

  /// No description provided for @sharingUnlikeFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to remove like'**
  String get sharingUnlikeFailed;

  /// No description provided for @sharingLikeFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to like'**
  String get sharingLikeFailed;

  /// No description provided for @sharingDeleteTitle.
  ///
  /// In en, this message translates to:
  /// **'Delete post'**
  String get sharingDeleteTitle;

  /// No description provided for @sharingDeleteConfirmBody2.
  ///
  /// In en, this message translates to:
  /// **'Delete? This can\'t be undone.'**
  String get sharingDeleteConfirmBody2;

  /// No description provided for @sharingDeleteFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to delete'**
  String get sharingDeleteFailed;

  /// No description provided for @sharingDetailTitle.
  ///
  /// In en, this message translates to:
  /// **'Post'**
  String get sharingDetailTitle;

  /// No description provided for @sharingReport.
  ///
  /// In en, this message translates to:
  /// **'Report'**
  String get sharingReport;

  /// No description provided for @sharingLoadFailed.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load the post'**
  String get sharingLoadFailed;

  /// No description provided for @sharingComments.
  ///
  /// In en, this message translates to:
  /// **'Comments'**
  String get sharingComments;

  /// No description provided for @sharingCommentHint.
  ///
  /// In en, this message translates to:
  /// **'Write a comment'**
  String get sharingCommentHint;

  /// No description provided for @sharingNoComments.
  ///
  /// In en, this message translates to:
  /// **'Be the first to comment'**
  String get sharingNoComments;

  /// No description provided for @sharingCommentsDisabled.
  ///
  /// In en, this message translates to:
  /// **'Comments are disabled for this post'**
  String get sharingCommentsDisabled;

  /// No description provided for @emptyDefault.
  ///
  /// In en, this message translates to:
  /// **'No data.'**
  String get emptyDefault;

  /// No description provided for @mypageTitle.
  ///
  /// In en, this message translates to:
  /// **'My page'**
  String get mypageTitle;

  /// No description provided for @mypagePartialError.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load some info'**
  String get mypagePartialError;

  /// No description provided for @mypageViewProfile.
  ///
  /// In en, this message translates to:
  /// **'View profile'**
  String get mypageViewProfile;

  /// No description provided for @withdrawTitle.
  ///
  /// In en, this message translates to:
  /// **'Delete account'**
  String get withdrawTitle;

  /// No description provided for @withdrawConfirm.
  ///
  /// In en, this message translates to:
  /// **'Withdraw'**
  String get withdrawConfirm;

  /// No description provided for @withdrawBody.
  ///
  /// In en, this message translates to:
  /// **'When you withdraw, your account is deactivated, and your personal data and records are kept for 2 years per applicable law, then automatically deleted.\n\nIf you log in again with the same Kakao account within that period, your account and records are restored.\n\nAre you sure you want to withdraw?'**
  String get withdrawBody;

  /// No description provided for @qmNotifications.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get qmNotifications;

  /// No description provided for @qmMyPraise.
  ///
  /// In en, this message translates to:
  /// **'My praise'**
  String get qmMyPraise;

  /// No description provided for @qmSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get qmSettings;

  /// No description provided for @qmSongCount.
  ///
  /// In en, this message translates to:
  /// **'{count} songs'**
  String qmSongCount(int count);

  /// No description provided for @statsTitle.
  ///
  /// In en, this message translates to:
  /// **'My meditation'**
  String get statsTitle;

  /// No description provided for @statsWeek.
  ///
  /// In en, this message translates to:
  /// **'This week'**
  String get statsWeek;

  /// No description provided for @statsMonth.
  ///
  /// In en, this message translates to:
  /// **'This month'**
  String get statsMonth;

  /// No description provided for @statsStreak.
  ///
  /// In en, this message translates to:
  /// **'Streak'**
  String get statsStreak;

  /// No description provided for @statsDays.
  ///
  /// In en, this message translates to:
  /// **'{days} days'**
  String statsDays(int days);

  /// No description provided for @notifMarkAllRead.
  ///
  /// In en, this message translates to:
  /// **'Mark all read'**
  String get notifMarkAllRead;

  /// No description provided for @notifUnreadOnly.
  ///
  /// In en, this message translates to:
  /// **'Unread only'**
  String get notifUnreadOnly;

  /// No description provided for @notifEmpty.
  ///
  /// In en, this message translates to:
  /// **'No notifications'**
  String get notifEmpty;

  /// No description provided for @timeJustNow.
  ///
  /// In en, this message translates to:
  /// **'Just now'**
  String get timeJustNow;

  /// No description provided for @timeMinutesAgo.
  ///
  /// In en, this message translates to:
  /// **'{minutes} min ago'**
  String timeMinutesAgo(int minutes);

  /// No description provided for @timeHoursAgo.
  ///
  /// In en, this message translates to:
  /// **'{hours} hr ago'**
  String timeHoursAgo(int hours);

  /// No description provided for @timeDaysAgo.
  ///
  /// In en, this message translates to:
  /// **'{days} days ago'**
  String timeDaysAgo(int days);

  /// No description provided for @settingsNotification.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get settingsNotification;

  /// No description provided for @settingsNotificationDesc.
  ///
  /// In en, this message translates to:
  /// **'Receive push notifications'**
  String get settingsNotificationDesc;

  /// No description provided for @settingsFontSize.
  ///
  /// In en, this message translates to:
  /// **'Font size'**
  String get settingsFontSize;

  /// No description provided for @settingsFontSizeDesc.
  ///
  /// In en, this message translates to:
  /// **'Set the body text size'**
  String get settingsFontSizeDesc;

  /// No description provided for @settingsFontSmall.
  ///
  /// In en, this message translates to:
  /// **'Small'**
  String get settingsFontSmall;

  /// No description provided for @settingsFontMedium.
  ///
  /// In en, this message translates to:
  /// **'Medium'**
  String get settingsFontMedium;

  /// No description provided for @settingsFontLarge.
  ///
  /// In en, this message translates to:
  /// **'Large'**
  String get settingsFontLarge;

  /// No description provided for @settingsTts.
  ///
  /// In en, this message translates to:
  /// **'TTS reading settings'**
  String get settingsTts;

  /// No description provided for @settingsTtsDesc.
  ///
  /// In en, this message translates to:
  /// **'Set the reading voice and scope (passage/commentary)'**
  String get settingsTtsDesc;

  /// No description provided for @praiseTitle.
  ///
  /// In en, this message translates to:
  /// **'Praise'**
  String get praiseTitle;

  /// No description provided for @praiseMyTab.
  ///
  /// In en, this message translates to:
  /// **'My praise'**
  String get praiseMyTab;

  /// No description provided for @praiseCurationTab.
  ///
  /// In en, this message translates to:
  /// **'Curation'**
  String get praiseCurationTab;

  /// No description provided for @praiseMyEmpty.
  ///
  /// In en, this message translates to:
  /// **'No saved praise songs\nSave songs from Curation'**
  String get praiseMyEmpty;

  /// No description provided for @praiseDeleted.
  ///
  /// In en, this message translates to:
  /// **'Praise song deleted'**
  String get praiseDeleted;

  /// No description provided for @praiseCurationEmpty.
  ///
  /// In en, this message translates to:
  /// **'No curated songs'**
  String get praiseCurationEmpty;

  /// No description provided for @praiseSaved.
  ///
  /// In en, this message translates to:
  /// **'Saved to my praise'**
  String get praiseSaved;

  /// No description provided for @praiseAlreadySaved.
  ///
  /// In en, this message translates to:
  /// **'Already saved'**
  String get praiseAlreadySaved;

  /// No description provided for @profileTitle.
  ///
  /// In en, this message translates to:
  /// **'Profile'**
  String get profileTitle;

  /// No description provided for @profileEmail.
  ///
  /// In en, this message translates to:
  /// **'Email'**
  String get profileEmail;

  /// No description provided for @profileJoinDate.
  ///
  /// In en, this message translates to:
  /// **'Joined'**
  String get profileJoinDate;

  /// No description provided for @profileLogout.
  ///
  /// In en, this message translates to:
  /// **'Log out'**
  String get profileLogout;

  /// No description provided for @profileNickname.
  ///
  /// In en, this message translates to:
  /// **'Nickname'**
  String get profileNickname;

  /// No description provided for @profileNewNickname.
  ///
  /// In en, this message translates to:
  /// **'New nickname'**
  String get profileNewNickname;

  /// No description provided for @profileNicknameHint2.
  ///
  /// In en, this message translates to:
  /// **'2-20 chars'**
  String get profileNicknameHint2;

  /// No description provided for @profileNicknameAvailable.
  ///
  /// In en, this message translates to:
  /// **'This nickname is available.'**
  String get profileNicknameAvailable;

  /// No description provided for @profileNicknameTaken.
  ///
  /// In en, this message translates to:
  /// **'This nickname is already in use.'**
  String get profileNicknameTaken;

  /// No description provided for @profileChange.
  ///
  /// In en, this message translates to:
  /// **'Change'**
  String get profileChange;

  /// No description provided for @profileNicknameChanged.
  ///
  /// In en, this message translates to:
  /// **'Nickname changed.'**
  String get profileNicknameChanged;

  /// No description provided for @profileNicknameChangeFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to change nickname.'**
  String get profileNicknameChangeFailed;

  /// No description provided for @profileLogoutFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to log out.'**
  String get profileLogoutFailed;

  /// No description provided for @profileWithdrawFailed.
  ///
  /// In en, this message translates to:
  /// **'Failed to process withdrawal.'**
  String get profileWithdrawFailed;

  /// No description provided for @ttsVoice.
  ///
  /// In en, this message translates to:
  /// **'Reading voice'**
  String get ttsVoice;

  /// No description provided for @ttsVoiceDesc.
  ///
  /// In en, this message translates to:
  /// **'Set the voice that reads the QT passage'**
  String get ttsVoiceDesc;

  /// No description provided for @ttsReadBible.
  ///
  /// In en, this message translates to:
  /// **'Read passage (Korean)'**
  String get ttsReadBible;

  /// No description provided for @ttsReadBibleDesc.
  ///
  /// In en, this message translates to:
  /// **'Reads the Korean QT passage'**
  String get ttsReadBibleDesc;

  /// No description provided for @ttsReadExplanation.
  ///
  /// In en, this message translates to:
  /// **'Read commentary'**
  String get ttsReadExplanation;

  /// No description provided for @ttsReadExplanationDesc.
  ///
  /// In en, this message translates to:
  /// **'Reads the commentary. If on together with passage reading, it reads after the passage'**
  String get ttsReadExplanationDesc;

  /// No description provided for @ttsAtLeastOne.
  ///
  /// In en, this message translates to:
  /// **'At least one of passage or commentary must be on'**
  String get ttsAtLeastOne;

  /// No description provided for @ttsServerError.
  ///
  /// In en, this message translates to:
  /// **'Can\'t connect to the TTS server'**
  String get ttsServerError;

  /// No description provided for @ttsCustomVoice.
  ///
  /// In en, this message translates to:
  /// **'Custom voice'**
  String get ttsCustomVoice;

  /// No description provided for @ttsDefaultVoice.
  ///
  /// In en, this message translates to:
  /// **'Default voice'**
  String get ttsDefaultVoice;

  /// No description provided for @ttsFinetuned.
  ///
  /// In en, this message translates to:
  /// **'Trained'**
  String get ttsFinetuned;

  /// No description provided for @ttsVoicesLoading.
  ///
  /// In en, this message translates to:
  /// **'Loading the voice list'**
  String get ttsVoicesLoading;

  /// No description provided for @ttsVoicesError.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load the voice list'**
  String get ttsVoicesError;

  /// No description provided for @catMeditation.
  ///
  /// In en, this message translates to:
  /// **'Meditation'**
  String get catMeditation;

  /// No description provided for @catSermon.
  ///
  /// In en, this message translates to:
  /// **'Sermon'**
  String get catSermon;

  /// No description provided for @catPrayer.
  ///
  /// In en, this message translates to:
  /// **'Prayer'**
  String get catPrayer;

  /// No description provided for @catGratitude.
  ///
  /// In en, this message translates to:
  /// **'Gratitude'**
  String get catGratitude;

  /// No description provided for @catRepentance.
  ///
  /// In en, this message translates to:
  /// **'Repentance'**
  String get catRepentance;
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
