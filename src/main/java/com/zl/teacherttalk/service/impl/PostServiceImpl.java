package com.zl.teacherttalk.service.impl;

import com.zl.teacherttalk.common.BusinessException;
import com.zl.teacherttalk.common.ErrorCode;
import com.zl.teacherttalk.common.PageResult;
import com.zl.teacherttalk.dto.PostCreateRequest;
import com.zl.teacherttalk.dto.PostListRow;
import com.zl.teacherttalk.dto.PostListItemResponse;
import com.zl.teacherttalk.dto.PostResponse;
import com.zl.teacherttalk.dto.PostThumbnailRow;
import com.zl.teacherttalk.entity.Post;
import com.zl.teacherttalk.entity.PostImage;
import com.zl.teacherttalk.mapper.PostImageMapper;
import com.zl.teacherttalk.mapper.PostMapper;
import com.zl.teacherttalk.security.CurrentUserContext;
import com.zl.teacherttalk.service.FileStorageService;
import com.zl.teacherttalk.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostMapper postMapper;
    private final PostImageMapper postImageMapper;
    private final FileStorageService fileStorageService;

    public PostServiceImpl(
            PostMapper postMapper,
            PostImageMapper postImageMapper,
            FileStorageService fileStorageService
    ) {
        this.postMapper = postMapper;
        this.postImageMapper = postImageMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public PageResult<PostListItemResponse> listPosts(int page, int size) {
        Long userId = requireExistingUserId();
        long offset = (long) (page - 1) * size;
        long total = postMapper.countPosts();
        List<PostListRow> rows = postMapper.selectPostPage(offset, size, userId);
        Map<Long, List<String>> thumbnailsByPostId = loadThumbnails(rows);
        List<PostListItemResponse> posts = rows.stream()
                .map(row -> PostListItemResponse.from(
                        row,
                        thumbnailsByPostId.getOrDefault(row.getId(), List.of())
                ))
                .toList();
        return PageResult.of(posts, page, size, total);
    }

    @Override
    @Transactional
    public PostResponse createPost(PostCreateRequest request) {
        Long userId = requireExistingUserId();
        List<String> imageUrls = normalizeAndValidateImageUrls(request.imageUrls());
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

        savePostImages(post.getId(), imageUrls, now);

        log.info(
                "帖子发布成功: postId={}, userId={}, imageCount={}",
                post.getId(),
                userId,
                imageUrls.size()
        );
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                imageUrls,
                post.getUserId(),
                post.getLikeCount(),
                post.getFavoriteCount(),
                post.getCommentCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private Long requireExistingUserId() {
        Long userId = CurrentUserContext.requireUserId();
        if (!postMapper.userExists(userId)) {
            log.warn("当前用户不存在: userId={}", userId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private Map<Long, List<String>> loadThumbnails(List<PostListRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }

        List<Long> postIds = rows.stream()
                .map(PostListRow::getId)
                .toList();
        Map<Long, List<String>> thumbnailsByPostId = new HashMap<>();
        for (PostThumbnailRow image : postMapper.selectPostThumbnails(postIds)) {
            String thumbnailUrl = toThumbnailUrl(image.getUrl());
            thumbnailsByPostId
                    .computeIfAbsent(image.getPostId(), ignored -> new ArrayList<>())
                    .add(thumbnailUrl);
        }
        return thumbnailsByPostId;
    }

    private String toThumbnailUrl(String originalUrl) {
        int slashIndex = originalUrl.lastIndexOf('/');
        int dotIndex = originalUrl.lastIndexOf('.');
        if (dotIndex <= slashIndex) {
            return originalUrl + "_thumb.jpg";
        }
        return originalUrl.substring(0, dotIndex) + "_thumb.jpg";
    }

    private void savePostImages(Long postId, List<String> imageUrls, LocalDateTime now) {
        for (int index = 0; index < imageUrls.size(); index++) {
            PostImage postImage = new PostImage();
            postImage.setPostId(postId);
            postImage.setUrl(imageUrls.get(index));
            postImage.setSortOrder(index);
            postImage.setCreatedAt(now);
            postImage.setUpdatedAt(now);
            if (postImageMapper.insert(postImage) != 1) {
                throw new IllegalStateException("帖子图片写入失败");
            }
        }
    }

    private List<String> normalizeAndValidateImageUrls(List<String> imageUrls) {
        if (imageUrls.size() > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "最多上传 5 张图片");
        }

        List<String> normalizedUrls = new ArrayList<>(imageUrls.size());
        Set<String> uniqueUrls = new HashSet<>();
        for (String rawImageUrl : imageUrls) {
            if (rawImageUrl == null || rawImageUrl.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址不能为空");
            }
            String imageUrl = rawImageUrl.strip();
            if (!fileStorageService.isOriginalImageAvailable(imageUrl)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址无效");
            }
            if (!uniqueUrls.add(imageUrl)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能重复");
            }
            normalizedUrls.add(imageUrl);
        }
        return normalizedUrls;
    }
}
