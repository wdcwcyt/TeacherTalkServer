package com.zl.teacherttalk.service.impl;

import com.zl.teacherttalk.common.BusinessException;
import com.zl.teacherttalk.common.ErrorCode;
import com.zl.teacherttalk.dto.PostCreateRequest;
import com.zl.teacherttalk.dto.PostResponse;
import com.zl.teacherttalk.entity.Post;
import com.zl.teacherttalk.mapper.PostMapper;
import com.zl.teacherttalk.security.CurrentUserContext;
import com.zl.teacherttalk.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostMapper postMapper;

    public PostServiceImpl(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Override
    @Transactional
    public PostResponse createPost(PostCreateRequest request) {
        Long userId = CurrentUserContext.requireUserId();
        if (!postMapper.userExists(userId)) {
            log.warn("本地开发用户不存在: userId={}", userId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        LocalDateTime now = LocalDateTime.now();
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setLikeCount(0);
        post.setFavoriteCount(0);
        post.setCommentCount(0);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);

        int affectedRows = postMapper.insert(post);
        if (affectedRows != 1 || post.getId() == null) {
            throw new IllegalStateException("帖子写入失败");
        }

        log.info("帖子发布成功: postId={}, userId={}", post.getId(), userId);
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getUserId(),
                post.getLikeCount(),
                post.getFavoriteCount(),
                post.getCommentCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
