package com.zl.teacherttalk.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostListItemResponse(
        Long id,
        String title,
        String contentPreview,
        Long authorId,
        String authorNickname,
        String authorAvatarUrl,
        List<String> thumbnailUrls,
        int likeCount,
        int favoriteCount,
        int commentCount,
        boolean liked,
        boolean favorited,
        LocalDateTime createdAt
) {

    public PostListItemResponse {
        thumbnailUrls = List.copyOf(thumbnailUrls);
    }

    public static PostListItemResponse from(
            PostListRow row,
            List<String> thumbnailUrls
    ) {
        return new PostListItemResponse(
                row.getId(),
                row.getTitle(),
                row.getContentPreview(),
                row.getAuthorId(),
                row.getAuthorNickname(),
                row.getAuthorAvatarUrl(),
                thumbnailUrls,
                row.getLikeCount(),
                row.getFavoriteCount(),
                row.getCommentCount(),
                Boolean.TRUE.equals(row.getLiked()),
                Boolean.TRUE.equals(row.getFavorited()),
                row.getCreatedAt()
        );
    }
}
