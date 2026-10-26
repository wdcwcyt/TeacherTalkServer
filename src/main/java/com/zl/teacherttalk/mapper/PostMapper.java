package com.zl.teacherttalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zl.teacherttalk.dto.PostListRow;
import com.zl.teacherttalk.dto.PostThumbnailRow;
import com.zl.teacherttalk.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT EXISTS(SELECT 1 FROM `user` WHERE id = #{userId})")
    boolean userExists(@Param("userId") Long userId);

    long countPosts();

    List<PostListRow> selectPostPage(
            @Param("offset") long offset,
            @Param("limit") int limit,
            @Param("currentUserId") Long currentUserId
    );

    List<PostThumbnailRow> selectPostThumbnails(@Param("postIds") List<Long> postIds);
}
