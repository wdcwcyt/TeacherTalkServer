package com.zl.teacherttalk.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String title,
        String content,
        List<String> imageUrls,
        Long authorId,
        int likeCount,
        int favoriteCount,
        int commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public PostResponse {
        title = title == null ? "" : title;
        imageUrls = List.copyOf(imageUrls);
    }
}
