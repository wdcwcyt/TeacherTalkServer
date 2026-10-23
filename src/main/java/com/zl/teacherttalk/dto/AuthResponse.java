package com.zl.teacherttalk.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
