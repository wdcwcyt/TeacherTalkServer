package com.zl.teacherttalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 100, message = "不能超过 100 个字符")
        String title,

        @NotBlank(message = "不能为空")
        @Size(max = 5000, message = "不能超过 5000 个字符")
        String content
) {

    public PostCreateRequest {
        title = title == null ? null : title.strip();
        content = content == null ? null : content.strip();
    }
}
