package com.zl.teacherttalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zl.teacherttalk.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT id, phone, password_hash, nickname, avatar_url, role, created_at, updated_at
            FROM `user`
            WHERE phone = #{phone}
            LIMIT 1
            """)
    User findByPhone(@Param("phone") String phone);
}
