package com.zl.teacherttalk.controller;

import com.zl.teacherttalk.common.PageResult;
import com.zl.teacherttalk.common.Result;
import com.zl.teacherttalk.dto.PostCreateRequest;
import com.zl.teacherttalk.dto.PostListItemResponse;
import com.zl.teacherttalk.dto.PostResponse;
import com.zl.teacherttalk.service.PostService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Result<PageResult<PostListItemResponse>> listPosts(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "必须从 1 开始")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "不能小于 1")
            @Max(value = 50, message = "不能超过 50")
            int size
    ) {
        return Result.success(postService.listPosts(page, size));
    }

    @PostMapping
    public Result<PostResponse> createPost(@Valid @RequestBody PostCreateRequest request) {
        return Result.success(postService.createPost(request));
    }
}
