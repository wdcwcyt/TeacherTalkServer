package com.zl.teacherttalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "格式不正确")
        String phone,

        @NotBlank(message = "不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "必须是 6 位数字")
        String code,

        @Size(max = 20, message = "不能超过 20 个字符")
        String nickname,

        @NotBlank(message = "不能为空")
        String role
) {

    public RegisterRequest {
        phone = phone == null ? null : phone.strip();
        code = code == null ? null : code.strip();
        nickname = nickname == null ? null : nickname.strip();
        role = role == null ? null : role.strip();
    }
}
