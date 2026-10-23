package com.zl.teacherttalk.dto;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        int likeCount,
        int favoriteCount,
        int commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
