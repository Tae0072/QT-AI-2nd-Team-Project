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
  String get commonBack => 'Back';

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

  @override
  String get noteModeWrite => 'New';

  @override
  String get noteShareTooltip => 'Share';

  @override
  String get noteShared => 'Shared';

  @override
  String get noteNoContent => '(No content)';

  @override
  String get noteDeleteConfirmTitle => 'Delete this note?';

  @override
  String get noteDeleteConfirmBody => 'Deleted notes cannot be recovered.';

  @override
  String get noteDeleted => 'Deleted';

  @override
  String get noteDeleteFailed => 'Failed to delete. Please try again.';

  @override
  String get noteSectionFelt => 'Reflections';

  @override
  String get noteSectionVerse => 'Verse to remember';

  @override
  String get noteSectionApply => 'How to apply';

  @override
  String get noteSectionPray => 'Prayer';

  @override
  String get noteQuotedVerses => 'Quoted verses';

  @override
  String get noteLoadFailed => 'Couldn\'t load the note';

  @override
  String get noteEditTitleLabel => 'Title (optional)';

  @override
  String get noteEditBodyLabel => 'Content';

  @override
  String get noteEditBodyRequired => 'Please enter the content';

  @override
  String get noteEditTitleOrBodyRequired => 'Please enter a title or content';

  @override
  String get noteSaved => 'Saved';

  @override
  String get noteDraftSaved => 'Saved as draft';

  @override
  String get noteSaveFailed => 'Failed to save. Please try again.';

  @override
  String get notePublishTooltip => 'Share to feed';

  @override
  String get notePublishSheetTitle => 'Share to nickname feed';

  @override
  String get notePublishNicknameNotice =>
      'Your nickname will be shown when you share.';

  @override
  String get notePublishCommentsLabel => 'Allow comments';

  @override
  String get notePublishConfirm => 'Share';

  @override
  String get notePublishNeedSave => 'Only saved notes can be shared';

  @override
  String get notePublishSuccess => 'Shared to the feed';

  @override
  String get notePublishFailed => 'Failed to share. Please try again';

  @override
  String get notePublishAlready => 'This note is already shared';

  @override
  String get notePublishView => 'View';

  @override
  String get fmtBold => 'Bold';

  @override
  String get fmtItalic => 'Italic';

  @override
  String get fmtHeading => 'Heading';

  @override
  String get fmtList => 'List';

  @override
  String get fmtQuote => 'Quote';

  @override
  String get fmtCheckbox => 'Checkbox';

  @override
  String get fmtDivider => 'Divider';

  @override
  String calSavedThisMonth(int days) {
    return 'Saved $days days this month';
  }

  @override
  String calNoteCount(int count) {
    return '$count notes';
  }

  @override
  String get calDayChecklistTitle => 'Notes written this day';

  @override
  String calStreak(int days) {
    return '$days-day streak 🔥';
  }

  @override
  String get noteShareAsText => 'Share as text';

  @override
  String get noteShareAsImage => 'Share as image';

  @override
  String get noteShareImageFailed => 'Failed to share image';

  @override
  String get navToday => 'QT';

  @override
  String get navBible => 'Bible';

  @override
  String get navShare => 'Sharing';

  @override
  String get navNote => 'Notes';

  @override
  String get navMy => 'My';

  @override
  String get ttsRead => 'Read passage';

  @override
  String get ttsStop => 'Stop reading';

  @override
  String get ttsNoExplanationInfo => 'No commentary info for today\'s QT';

  @override
  String get ttsNoExplanationReady => 'No commentary is ready yet';

  @override
  String get ttsOnlyBodyNoExplanation =>
      'No commentary yet, reading the passage only';

  @override
  String get ttsExplanationLoadFailed => 'Couldn\'t load the commentary';

  @override
  String get ttsOnlyBodyExplanationFailed =>
      'Couldn\'t load commentary, reading the passage only';

  @override
  String get ttsTokenMissing => 'TTS token is not set';

  @override
  String get ttsTurnOnReadItems =>
      'Turn on items to read (passage/commentary) in settings';

  @override
  String get ttsPrepareFailed => 'Couldn\'t prepare the audio';

  @override
  String get onbSkip => 'Skip';

  @override
  String get onbNext => 'Next';

  @override
  String get onbStart => 'Get started';

  @override
  String get commonRefresh => 'Refresh';

  @override
  String get bibleTodayQt => 'Today\'s QT';

  @override
  String get bibleTodayLoading => 'Loading today\'s passage...';

  @override
  String get bibleTodayLoadError => 'Couldn\'t load today\'s passage.';

  @override
  String get bibleExplanation => 'Commentary';

  @override
  String get bibleSimulator => 'Animation';

  @override
  String get qtVideoTitle => 'QT Video';

  @override
  String get qtVideoRetry => 'Reload';

  @override
  String get videoBack => 'Back';

  @override
  String get videoPlay => 'Play';

  @override
  String get videoPause => 'Pause';

  @override
  String get videoSpeed => 'Speed';

  @override
  String get videoFullscreen => 'Fullscreen';

  @override
  String get bibleMeditationNote => 'Write meditation note';

  @override
  String bibleComingSoon(String feature) {
    return 'The $feature screen is coming soon.';
  }

  @override
  String get bibleBrowserTitle => 'Bible';

  @override
  String get bibleChapterError =>
      'Chapter and verse must be numbers of 1 or more.';

  @override
  String get bibleBooksLoading => 'Loading the list of books...';

  @override
  String get bibleBooksLoadError => 'Couldn\'t load the list of books.';

  @override
  String get bibleBooksEmpty => 'No books available.';

  @override
  String get bibleChapter => 'Chapter';

  @override
  String get bibleVerseFrom => 'From verse';

  @override
  String get bibleVerseTo => 'To verse';

  @override
  String get bibleSame => 'Same';

  @override
  String get bibleSearch => 'Search';

  @override
  String get bibleVersesLoadError => 'Couldn\'t load the passage.';

  @override
  String get bibleSelectPrompt => 'Select a passage to look up.';

  @override
  String get sharingMine => 'My sharing';

  @override
  String get sharingSearchHint => 'Search posts';

  @override
  String get sharingFeedEmpty => 'No posts yet';

  @override
  String get sharingMineEmpty => 'You haven\'t shared anything';

  @override
  String get sharingHidden => 'Hidden';

  @override
  String get sharingShow => 'Make public again';

  @override
  String get sharingHide => 'Hide';

  @override
  String get sharingActionFailed => 'Action failed. Please try again.';

  @override
  String get sharingDeleteConfirmTitle => 'Delete this post?';

  @override
  String get sharingDeleteConfirmBody => 'This can\'t be undone.';

  @override
  String get sharingCommentFailed => 'Failed to post comment';

  @override
  String get sharingCommentDeleteFailed => 'Failed to delete comment';

  @override
  String get reportSpam => 'Spam/Ads';

  @override
  String get reportHate => 'Hate/Abuse';

  @override
  String get reportSexual => 'Sexual content';

  @override
  String get reportEtc => 'Other';

  @override
  String get sharingReportPrompt => 'Select a report reason';

  @override
  String get sharingReportSubmitted => 'Report submitted';

  @override
  String get sharingReportFailed => 'Failed to report';

  @override
  String get sharingUnlikeFailed => 'Failed to remove like';

  @override
  String get sharingLikeFailed => 'Failed to like';

  @override
  String get sharingDeleteTitle => 'Delete post';

  @override
  String get sharingDeleteConfirmBody2 => 'Delete? This can\'t be undone.';

  @override
  String get sharingDeleteFailed => 'Failed to delete';

  @override
  String get sharingDetailTitle => 'Post';

  @override
  String get sharingReport => 'Report';

  @override
  String get sharingLoadFailed => 'Couldn\'t load the post';

  @override
  String get sharingComments => 'Comments';

  @override
  String get sharingCommentHint => 'Write a comment';

  @override
  String get sharingNoComments => 'Be the first to comment';

  @override
  String get sharingCommentsDisabled => 'Comments are disabled for this post';

  @override
  String get emptyDefault => 'No data.';

  @override
  String get mypageTitle => 'My page';

  @override
  String get mypagePartialError => 'Couldn\'t load some info';

  @override
  String get mypageViewProfile => 'View profile';

  @override
  String get withdrawTitle => 'Delete account';

  @override
  String get withdrawConfirm => 'Withdraw';

  @override
  String get withdrawBody =>
      'When you withdraw, your account is deactivated, and your personal data and records are kept for 2 years per applicable law, then automatically deleted.\n\nIf you log in again with the same Kakao account within that period, your account and records are restored.\n\nAre you sure you want to withdraw?';

  @override
  String get qmNotifications => 'Notifications';

  @override
  String get qmMyPraise => 'My praise';

  @override
  String get qmSettings => 'Settings';

  @override
  String qmSongCount(int count) {
    return '$count songs';
  }

  @override
  String get statsTitle => 'My meditation';

  @override
  String get statsWeek => 'This week';

  @override
  String get statsMonth => 'This month';

  @override
  String get statsStreak => 'Streak';

  @override
  String statsDays(int days) {
    return '$days days';
  }

  @override
  String get notifMarkAllRead => 'Mark all read';

  @override
  String get notifUnreadOnly => 'Unread only';

  @override
  String get notifEmpty => 'No notifications';

  @override
  String get timeJustNow => 'Just now';

  @override
  String timeMinutesAgo(int minutes) {
    return '$minutes min ago';
  }

  @override
  String timeHoursAgo(int hours) {
    return '$hours hr ago';
  }

  @override
  String timeDaysAgo(int days) {
    return '$days days ago';
  }

  @override
  String get settingsNotification => 'Notifications';

  @override
  String get settingsNotificationDesc => 'Receive push notifications';

  @override
  String get settingsFontSize => 'Font size';

  @override
  String get settingsFontSizeDesc => 'Set the body text size';

  @override
  String get settingsFontSmall => 'Small';

  @override
  String get settingsFontMedium => 'Medium';

  @override
  String get settingsFontLarge => 'Large';

  @override
  String get settingsTts => 'TTS reading settings';

  @override
  String get settingsTtsDesc =>
      'Set the reading voice and scope (passage/commentary)';

  @override
  String get praiseTitle => 'Praise';

  @override
  String get praiseMyTab => 'My praise';

  @override
  String get praiseCurationTab => 'Curation';

  @override
  String get praiseMyEmpty => 'No saved praise songs\nSave songs from Curation';

  @override
  String get praiseDeleted => 'Praise song deleted';

  @override
  String get praiseCurationEmpty => 'No curated songs';

  @override
  String get praiseSaved => 'Saved to my praise';

  @override
  String get praiseAlreadySaved => 'Already saved';

  @override
  String get profileTitle => 'Profile';

  @override
  String get profileEmail => 'Email';

  @override
  String get profileJoinDate => 'Joined';

  @override
  String get profileLogout => 'Log out';

  @override
  String get profileNickname => 'Nickname';

  @override
  String get profileNewNickname => 'New nickname';

  @override
  String get profileNicknameHint2 => '2-20 chars';

  @override
  String get profileNicknameAvailable => 'This nickname is available.';

  @override
  String get profileNicknameTaken => 'This nickname is already in use.';

  @override
  String get profileChange => 'Change';

  @override
  String get profileNicknameChanged => 'Nickname changed.';

  @override
  String get profileNicknameChangeFailed => 'Failed to change nickname.';

  @override
  String get profileLogoutFailed => 'Failed to log out.';

  @override
  String get profileWithdrawFailed => 'Failed to process withdrawal.';

  @override
  String get ttsVoice => 'Reading voice';

  @override
  String get ttsVoiceDesc => 'Set the voice that reads the QT passage';

  @override
  String get ttsReadBible => 'Read passage (Korean)';

  @override
  String get ttsReadBibleDesc => 'Reads the Korean QT passage';

  @override
  String get ttsReadExplanation => 'Read commentary';

  @override
  String get ttsReadExplanationDesc =>
      'Reads the commentary. If on together with passage reading, it reads after the passage';

  @override
  String get ttsAtLeastOne =>
      'At least one of passage or commentary must be on';

  @override
  String get ttsServerError => 'Can\'t connect to the TTS server';

  @override
  String get ttsCustomVoice => 'Custom voice';

  @override
  String get ttsDefaultVoice => 'Default voice';

  @override
  String get ttsFinetuned => 'Trained';

  @override
  String get ttsVoicesLoading => 'Loading the voice list';

  @override
  String get ttsVoicesError => 'Couldn\'t load the voice list';

  @override
  String get catMeditation => 'Meditation';

  @override
  String get catSermon => 'Sermon';

  @override
  String get catPrayer => 'Prayer';

  @override
  String get catGratitude => 'Gratitude';

  @override
  String get catRepentance => 'Repentance';
}
