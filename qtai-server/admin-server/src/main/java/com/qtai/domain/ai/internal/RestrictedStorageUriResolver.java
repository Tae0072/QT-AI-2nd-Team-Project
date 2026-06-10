package com.qtai.domain.ai.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Component
class RestrictedStorageUriResolver {

    private static final String RESTRICTED_SCHEME = "restricted";
    private static final String INVALID_URI = "AI_REVIEW_REFERENCE_INDEX_URI_INVALID";

    private final Path restrictedStorageRoot;

    @Autowired
    RestrictedStorageUriResolver(
            @Value("${qtai.validation.restricted-storage-root:./restricted}") String restrictedStorageRoot
    ) {
        this(Path.of(restrictedStorageRoot));
    }

    RestrictedStorageUriResolver(Path restrictedStorageRoot) {
        this.restrictedStorageRoot = restrictedStorageRoot.toAbsolutePath().normalize();
    }

    Path resolve(String restrictedUri) {
        if (restrictedUri == null || restrictedUri.isBlank()) {
            throw invalidUri();
        }

        URI uri = parseUri(restrictedUri);
        if (!RESTRICTED_SCHEME.equals(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
            throw invalidUri();
        }

        List<String> segments = new ArrayList<>();
        segments.add(uri.getHost());
        String path = uri.getPath();
        if (path != null && !path.isBlank()) {
            for (String segment : path.split("/")) {
                if (!segment.isBlank()) {
                    segments.add(segment);
                }
            }
        }
        if (segments.stream().anyMatch(RestrictedStorageUriResolver::isUnsafeSegment)) {
            throw invalidUri();
        }

        Path resolved = restrictedStorageRoot;
        for (String segment : segments) {
            resolved = resolved.resolve(segment);
        }
        resolved = resolved.normalize();
        if (!resolved.startsWith(restrictedStorageRoot)) {
            throw invalidUri();
        }
        return resolved;
    }

    private static URI parseUri(String restrictedUri) {
        try {
            return new URI(restrictedUri);
        } catch (URISyntaxException exception) {
            throw invalidUri();
        }
    }

    private static boolean isUnsafeSegment(String segment) {
        return ".".equals(segment) || "..".equals(segment) || segment.contains("\\");
    }

    private static BusinessException invalidUri() {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, INVALID_URI);
    }
}
