package com.a05.admin.mapper;

import com.a05.admin.entity.Resume;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

    @Select("SELECT * FROM resume WHERE id = #{resumeId} AND user_id = #{userId} LIMIT 1")
    Resume selectByUserIdAndResumeId(@Param("userId") Long userId, @Param("resumeId") Long resumeId);
}

