package com.qtai.domain.sharing.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.dto.SharingPostListItem;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import com.qtai.domain.sharing.api.dto.SharingPostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharingPostServiceTest {

    private SharingPostRepository sharingPostRepository;
    private PostLikeRepository postLikeRepository;
    private SharingPostService sharingPostService;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        sharingPostRepository = mock(SharingPostRepository.class);
        postLikeRepository = mock(PostLikeRepository.class);
        sharingPostService = new SharingPostService(sharingPostRepository, postLikeRepository);
        pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "publishedAt"));
    }

    @Test
    @DisplayName("나눔 글을 응답 필드로 매핑한다 (닉네임·제목·구절라벨·발행시각)")
    void list_mapsFields() {
        SharingPost post = sharingPost(1L, "하늘QT", "오늘의 묵상", "MEDITATION",
                "본문 내용", "창세기 1:1-5", 5, 2);
        when(sharingPostRepository.search(eq(SharingPostStatus.PUBLISHED), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1L));
        when(postLikeRepository.findLikedPostIds(eq(10L), anyCollection())).thenReturn(List.of());

        SharingPostListResponse response = sharingPostService.list(10L, null, null, pageable);

        assertThat(response.content()).hasSize(1);
        SharingPostListItem item = response.content().get(0);
        assertThat(item.id()).isEqualTo(1L);
        assertThat(item.nicknameSnapshot()).isEqualTo("하늘QT");
        assertThat(item.titleSnapshot()).isEqualTo("오늘의 묵상");
        assertThat(item.category()).isEqualTo("MEDITATION");
        assertThat(item.status()).isEqualTo("PUBLISHED");
        assertThat(item.verseSnapshot().rangeLabel()).isEqualTo("창세기 1:1-5");
        assertThat(item.bodyPreview()).isEqualTo("본문 내용");
        assertThat(item.likeCount()).isEqualTo(5);
        assertThat(item.commentCount()).isEqualTo(2);
        assertThat(item.publishedAt()).isNotNull();
    }

    @Test
    @DisplayName("likedByMe는 내가 좋아요 누른 글만 true다")
    void list_likedByMe_reflectsBatchResult() {
        SharingPost liked = sharingPost(1L, "하늘QT", "글1", "PRAYER", "본문1", null, 1, 0);
        SharingPost notLiked = sharingPost(2L, "은혜QT", "글2", "PRAYER", "본문2", null, 0, 0);
        when(sharingPostRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(liked, notLiked), pageable, 2L));
        when(postLikeRepository.findLikedPostIds(eq(10L), anyCollection())).thenReturn(List.of(1L));

        SharingPostListResponse response = sharingPostService.list(10L, null, null, pageable);

        assertThat(response.content().get(0).likedByMe()).isTrue();
        assertThat(response.content().get(1).likedByMe()).isFalse();
    }

    @Test
    @DisplayName("bodyPreview는 100자를 넘으면 잘리고 말줄임표가 붙는다")
    void list_bodyPreview_truncatesLongBody() {
        String longBody = "가".repeat(150);
        SharingPost post = sharingPost(1L, "하늘QT", "제목", "GRATITUDE", longBody, null, 0, 0);
        when(sharingPostRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1L));
        when(postLikeRepository.findLikedPostIds(any(), anyCollection())).thenReturn(List.of());

        SharingPostListResponse response = sharingPostService.list(10L, null, null, pageable);

        String preview = response.content().get(0).bodyPreview();
        assertThat(preview).hasSize(101); // 100자 + "…"
        assertThat(preview).endsWith("…");
    }

    @Test
    @DisplayName("q의 LIKE 와일드카드(%, _)를 이스케이프해서 Repository에 넘긴다")
    void list_escapesLikeWildcards() {
        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        when(sharingPostRepository.search(any(), any(), qCaptor.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        sharingPostService.list(10L, null, "50%_a", pageable);

        assertThat(qCaptor.getValue()).isEqualTo("50\\%\\_a");
    }

    @Test
    @DisplayName("정렬 publishedAt은 엔티티 createdAt으로 변환되고 응답엔 publishedAt으로 표기된다")
    void list_translatesSortField() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(sharingPostRepository.search(any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        SharingPostListResponse response = sharingPostService.list(10L, null, null, pageable);

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("publishedAt")).isNull();
        assertThat(response.sort()).isEqualTo("publishedAt,desc");
    }

    @Test
    @DisplayName("결과가 비면 likedByMe 배치 조회를 호출하지 않는다 (빈 IN 절 방지)")
    void list_emptyResult_skipsLikeQuery() {
        when(sharingPostRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        SharingPostListResponse response = sharingPostService.list(10L, null, null, pageable);

        assertThat(response.content()).isEmpty();
        verify(postLikeRepository, never()).findLikedPostIds(any(), anyCollection());
    }

    @Test
    @DisplayName("상세 조회: 전체 본문·rangeLabel을 매핑하고 verses는 빈 배열(v2)이다")
    void getDetail_mapsFullDetail() {
        SharingPost post = sharingPost(1L, "하늘QT", "오늘의 묵상", "MEDITATION",
                "전체 본문 내용", "창세기 1:1-5", 5, 2);
        when(sharingPostRepository.findByIdAndStatus(1L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(post));
        when(postLikeRepository.findLikedPostIds(eq(10L), anyCollection())).thenReturn(List.of(1L));

        SharingPostResponse response = sharingPostService.getDetail(10L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.bodySnapshot()).isEqualTo("전체 본문 내용");
        assertThat(response.verseSnapshot().rangeLabel()).isEqualTo("창세기 1:1-5");
        assertThat(response.verseSnapshot().verses()).isEmpty();
        assertThat(response.likedByMe()).isTrue();
        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(response.likeCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("상세 조회: PUBLISHED가 아니면(HIDDEN/DELETED/없음) 404 NOT_FOUND")
    void getDetail_notPublished_throwsNotFound() {
        when(sharingPostRepository.findByIdAndStatus(99L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingPostService.getDetail(10L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SHARING_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("상세 조회: ownedByMe는 작성자(memberId=99)일 때만 true다")
    void getDetail_ownedByMe_reflectsAuthorship() {
        SharingPost post = sharingPost(1L, "하늘QT", "글", "PRAYER", "본문", null, 0, 0);
        when(sharingPostRepository.findByIdAndStatus(1L, SharingPostStatus.PUBLISHED))
                .thenReturn(Optional.of(post));
        when(postLikeRepository.findLikedPostIds(any(), anyCollection())).thenReturn(List.of());

        // 헬퍼가 memberId=99로 글을 만듦
        assertThat(sharingPostService.getDetail(99L, 1L).ownedByMe()).isTrue();
        assertThat(sharingPostService.getDetail(10L, 1L).ownedByMe()).isFalse();
    }

    // ─────────────────────────────────────────────────────
    // 헬퍼 — SharingPost는 빌더/팩토리가 없어 reflection으로 필드를 채운다.
    // ─────────────────────────────────────────────────────

    private static SharingPost sharingPost(Long id, String nickname, String title, String category,
                                           String body, String verseLabel, int likeCount, int commentCount) {
        SharingPost post = new SharingPost();
        setField(post, "id", id);
        setField(post, "memberId", 99L);
        setField(post, "noteId", id);
        setField(post, "status", SharingPostStatus.PUBLISHED);
        setField(post, "snapshotTitle", title);
        setField(post, "snapshotBody", body);
        setField(post, "snapshotCategory", category);
        setField(post, "nicknameSnapshot", nickname);
        setField(post, "snapshotVerseLabel", verseLabel);
        setField(post, "commentsEnabled", true);
        setField(post, "likeCount", likeCount);
        setField(post, "commentCount", commentCount);
        setField(post, "createdAt", LocalDateTime.now());
        return post;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field: " + fieldName, e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
