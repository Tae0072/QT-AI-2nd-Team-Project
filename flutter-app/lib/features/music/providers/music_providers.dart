import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../../../core/config/app_config.dart';
import '../../../core/network/api_client.dart';
import '../../../core/storage/secure_storage.dart';
import '../../mypage/providers/mypage_providers.dart';
import '../models/music_track.dart';
import '../services/music_repository.dart';

/// 음악 카테고리 옵션.
const List<String> kMusicCategories = ['ALL', 'BGM', 'HYMN'];

/// MusicRepository provider.
final musicRepositoryProvider = Provider<MusicRepository>((ref) {
  return MusicRepository(ref.watch(dioProvider));
});

/// 전역 배경음악 상태.
class MusicState {
  /// 서버 설정/음원 로드 완료 여부.
  final bool initialized;

  /// 사용자 설정(기본 ON). 영구값은 서버 member_settings.music_enabled.
  final bool enabled;

  /// 현재 실제 재생 중 여부.
  final bool playing;

  /// 볼륨 0~100.
  final int volume;

  /// 재생 대상 ALL | BGM | HYMN (기본 BGM).
  final String category;

  /// 재생 가능한 음원 존재 여부(시딩 안 됐으면 false).
  final bool hasTracks;

  /// 현재 카테고리의 재생 목록(순서 = 플레이어 소스 순서).
  final List<MusicTrack> playlist;

  /// 현재 재생 중인 트랙 인덱스(playlist 기준).
  final int currentIndex;

  const MusicState({
    this.initialized = false,
    this.enabled = true,
    this.playing = false,
    this.volume = 70,
    this.category = 'BGM',
    this.hasTracks = false,
    this.playlist = const [],
    this.currentIndex = 0,
  });

  MusicState copyWith({
    bool? initialized,
    bool? enabled,
    bool? playing,
    int? volume,
    String? category,
    bool? hasTracks,
    List<MusicTrack>? playlist,
    int? currentIndex,
  }) {
    return MusicState(
      initialized: initialized ?? this.initialized,
      enabled: enabled ?? this.enabled,
      playing: playing ?? this.playing,
      volume: volume ?? this.volume,
      category: category ?? this.category,
      hasTracks: hasTracks ?? this.hasTracks,
      playlist: playlist ?? this.playlist,
      currentIndex: currentIndex ?? this.currentIndex,
    );
  }
}

/// 앱 전역 배경음악 컨트롤러.
///
/// - 하단바 5탭과 모든 하위/상위 화면에서 동일 인스턴스로 재생을 유지한다
///   (HomeScreen에 마운트되는 BackgroundMusicHost가 살아있는 동안 유지).
/// - 설정(켜기/끄기·볼륨·카테고리)은 서버 member_settings에 저장한다.
/// - 음원은 qtai-server에서 스트리밍한다(AudioSource에 JWT 헤더 첨부).
class MusicController extends StateNotifier<MusicState> {
  final Ref _ref;
  final AudioPlayer _player = AudioPlayer();

  bool _loading = false;
  List<MusicTrack> _allTracks = const [];

  MusicController(this._ref) : super(const MusicState()) {
    _player.playingStream.listen((playing) {
      if (mounted) state = state.copyWith(playing: playing);
    });
    _player.currentIndexStream.listen((index) {
      if (mounted && index != null) state = state.copyWith(currentIndex: index);
    });
  }

  /// 최초 1회: 서버 설정 로드 → 볼륨/플레이리스트 준비 → 켜져 있으면 자동 재생.
  Future<void> ensureInitialized() async {
    if (state.initialized || _loading) return;
    _loading = true;
    try {
      try {
        final s = await _ref.read(myPageRepositoryProvider).getSettings();
        state = state.copyWith(
          enabled: s.musicEnabled,
          volume: s.musicVolume,
          category: s.musicCategory,
          initialized: true,
        );
      } catch (_) {
        // 설정 조회 실패 시 기본값(ON/70/BGM) 유지하고 진행.
        state = state.copyWith(initialized: true);
      }

      await _player.setVolume(state.volume / 100.0);
      await _loadPlaylist();

      if (state.enabled && state.hasTracks) {
        _safePlay();
      }
    } finally {
      _loading = false;
    }
  }

  /// 카테고리에 맞는 음원으로 플레이리스트를 구성하고 반복(loop all) 설정.
  Future<void> _loadPlaylist() async {
    try {
      if (_allTracks.isEmpty) {
        _allTracks = await _ref.read(musicRepositoryProvider).getTracks();
      }
      final filtered = state.category == 'ALL'
          ? _allTracks
          : _allTracks.where((t) => t.category == state.category).toList();

      if (filtered.isEmpty) {
        state = state.copyWith(hasTracks: false, playlist: const []);
        return;
      }

      final token = await SecureStorage.getAccessToken();
      final headers =
          token != null ? <String, String>{'Authorization': 'Bearer $token'} : null;
      final base = AppConfig.instance.baseUrl; // 예: http://host:8080/api/v1

      final sources = filtered
          .map((t) => AudioSource.uri(
                Uri.parse('$base/music/tracks/${t.id}/stream'),
                headers: headers,
              ))
          .toList();

      await _player.setAudioSource(
        ConcatenatingAudioSource(children: sources),
        preload: false,
      );
      await _player.setLoopMode(LoopMode.all);
      state = state.copyWith(hasTracks: true, playlist: filtered, currentIndex: 0);
    } catch (_) {
      state = state.copyWith(hasTracks: false, playlist: const []);
    }
  }

  void _safePlay() async {
    try {
      await _player.play();
    } catch (_) {
      // 웹 브라우저는 사용자 제스처 전 자동재생이 막힐 수 있다 — 첫 터치에서 재생됨.
    }
  }

  /// 오늘의 QT 음표 버튼 토글: 재생/일시정지 + 설정값 저장.
  Future<void> toggle() async {
    await ensureInitialized();
    if (state.playing) {
      await _player.pause();
      await setEnabled(false);
    } else {
      await setEnabled(true);
      _safePlay();
    }
  }

  /// 켜기/끄기 설정 변경(서버 저장).
  Future<void> setEnabled(bool value) async {
    state = state.copyWith(enabled: value);
    if (value) {
      if (!state.hasTracks) await _loadPlaylist();
      _safePlay();
    } else {
      await _player.pause();
    }
    // 서버 저장은 fire-and-forget(UI 즉시 반영) — 실패 처리는 _persist 내부 책임.
    unawaited(_persist(musicEnabled: value));
  }

  /// 슬라이더 드래그 중 실시간 볼륨 반영(저장하지 않음).
  Future<void> previewVolume(int value) async {
    state = state.copyWith(volume: value);
    await _player.setVolume(value / 100.0);
  }

  /// 볼륨 확정(서버 저장).
  Future<void> commitVolume(int value) async {
    await previewVolume(value);
    unawaited(_persist(musicVolume: value));
  }

  /// 카테고리 변경(서버 저장 + 플레이리스트 재구성).
  Future<void> setCategory(String value) async {
    state = state.copyWith(category: value);
    await _loadPlaylist();
    if (state.enabled && state.hasTracks) _safePlay();
    unawaited(_persist(musicCategory: value));
  }

  /// 목록에서 특정 곡 선택 재생(현재 플레이리스트 인덱스 기준).
  Future<void> playByIndex(int index) async {
    await ensureInitialized();
    if (index < 0 || index >= state.playlist.length) return;
    try {
      await _player.seek(Duration.zero, index: index);
    } catch (_) {
      // ignore
    }
    state = state.copyWith(enabled: true, currentIndex: index);
    _safePlay();
    unawaited(_persist(musicEnabled: true));
  }

  /// 첫 사용자 제스처 알림 — 웹에서 자동재생이 막혔을 때 첫 터치에 재생을 시작한다.
  void notifyUserGesture() {
    if (state.enabled && state.hasTracks && !state.playing) {
      _safePlay();
    }
  }

  Future<void> _persist({
    bool? musicEnabled,
    int? musicVolume,
    String? musicCategory,
  }) async {
    try {
      await _ref.read(myPageRepositoryProvider).updateSettings(
            musicEnabled: musicEnabled,
            musicVolume: musicVolume,
            musicCategory: musicCategory,
          );
    } catch (_) {
      // 설정 저장 실패는 재생 동작을 막지 않는다(다음 변경 시 재시도).
    }
  }

  @override
  void dispose() {
    _player.dispose();
    super.dispose();
  }
}

/// 앱 전역 배경음악 컨트롤러 provider(앱 수명 동안 단일 인스턴스).
final musicControllerProvider =
    StateNotifierProvider<MusicController, MusicState>((ref) {
  return MusicController(ref);
});
