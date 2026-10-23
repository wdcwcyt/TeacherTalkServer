package com.zl.teacherttalk.security;

import com.zl.teacherttalk.common.BusinessException;
import com.zl.teacherttalk.common.ErrorCode;

public final class CurrentUserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void setUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        CURRENT_USER_ID.set(userId);
    }

    public static Long requireUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
