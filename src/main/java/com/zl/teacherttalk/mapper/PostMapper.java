package com.zl.teacherttalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zl.teacherttalk.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT EXISTS(SELECT 1 FROM `user` WHERE id = #{userId})")
    boolean userExists(@Param("userId") Long userId);
}
