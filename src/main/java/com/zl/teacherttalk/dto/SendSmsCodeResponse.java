package com.zl.teacherttalk.dto;

public record SendSmsCodeResponse(
        long expiresIn,
        long resendAfter
) {
}
