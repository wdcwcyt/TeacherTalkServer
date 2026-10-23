package com.zl.teacherttalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendSmsCodeRequest(
        @NotBlank(message = "不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "格式不正确")
        String phone
) {

    public SendSmsCodeRequest {
        phone = phone == null ? null : phone.strip();
    }
}
