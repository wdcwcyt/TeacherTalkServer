package com.zl.teacherttalk.controller;

import com.zl.teacherttalk.common.Result;
import com.zl.teacherttalk.dto.PostCreateRequest;
import com.zl.teacherttalk.dto.PostResponse;
import com.zl.teacherttalk.service.PostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public Result<PostResponse> createPost(@Valid @RequestBody PostCreateRequest request) {
        return Result.success(postService.createPost(request));
    }
}
