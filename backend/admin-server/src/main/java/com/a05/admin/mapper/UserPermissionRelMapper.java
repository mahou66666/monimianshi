package com.a05.admin.mapper;

import com.a05.admin.entity.UserPermissionRel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPermissionRelMapper extends BaseMapper<UserPermissionRel> {

    @Select("SELECT COUNT(1) FROM user_permission_rel WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    @Select("SELECT perm_id FROM user_permission_rel WHERE user_id = #{userId}")
    List<Long> listPermIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM user_permission_rel WHERE user_id = #{userId} AND perm_id = #{permId}")
    long countByUserIdAndPermId(@Param("userId") Long userId, @Param("permId") Long permId);

    @Delete("DELETE FROM user_permission_rel WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}

