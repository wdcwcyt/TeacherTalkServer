package com.zl.teacherttalk.common;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(List<T> list, int page, int size, long total) {

    public PageResult {
        list = List.copyOf(Objects.requireNonNull(list, "list 不能为空"));
    }

    public static <T> PageResult<T> of(List<T> list, int page, int size, long total) {
        return new PageResult<>(list, page, size, total);
    }
}
