package com.zl.teacherttalk.dto;

import com.zl.teacherttalk.entity.User;

public record UserResponse(
        Long id,
        String phone,
        String nickname,
        String avatarUrl,
        String role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getPhone(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }
}
