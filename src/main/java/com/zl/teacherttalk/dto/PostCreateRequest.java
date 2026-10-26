package com.zl.teacherttalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @Size(max = 100, message = "不能超过 100 个字符")
        String title,

        @NotBlank(message = "不能为空")
        @Size(max = 5000, message = "不能超过 5000 个字符")
        String content,

        @Size(max = 5, message = "最多上传 5 张图片")
        List<@NotBlank(message = "不能为空") String> imageUrls
) {

    public PostCreateRequest {
        title = title == null ? "" : title.strip();
        content = content == null ? null : content.strip();
        imageUrls = imageUrls == null ? List.of() : imageUrls;
    }
}
