package com.zl.teacherttalk.service;

import com.zl.teacherttalk.dto.PostCreateRequest;
import com.zl.teacherttalk.dto.PostResponse;

public interface PostService {

    PostResponse createPost(PostCreateRequest request);
}
