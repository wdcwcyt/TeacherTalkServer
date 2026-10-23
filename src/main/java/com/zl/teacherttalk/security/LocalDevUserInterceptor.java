package com.zl.teacherttalk.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Profile("local")
public class LocalDevUserInterceptor implements HandlerInterceptor {

    private final Long localUserId;

    public LocalDevUserInterceptor(@Value("${app.local-user-id}") Long localUserId) {
        if (localUserId == null || localUserId <= 0) {
            throw new IllegalStateException("app.local-user-id 必须是正整数");
        }
        this.localUserId = localUserId;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        CurrentUserContext.setUserId(localUserId);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        CurrentUserContext.clear();
    }
}
