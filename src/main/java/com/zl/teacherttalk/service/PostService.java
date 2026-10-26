package com.zl.teacherttalk.service;

import com.zl.teacherttalk.common.PageResult;
import com.zl.teacherttalk.dto.PostCreateRequest;
import com.zl.teacherttalk.dto.PostListItemResponse;
import com.zl.teacherttalk.dto.PostResponse;

public interface PostService {

    PageResult<PostListItemResponse> listPosts(int page, int size);

    PostResponse createPost(PostCreateRequest request);
}
