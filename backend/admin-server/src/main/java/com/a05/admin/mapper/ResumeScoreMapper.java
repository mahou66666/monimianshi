package com.a05.admin.mapper;

import com.a05.admin.entity.ResumeScore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResumeScoreMapper extends BaseMapper<ResumeScore> {

    @Select("SELECT * FROM resume_score WHERE user_id = #{userId} AND resume_id = #{resumeId} ORDER BY create_time DESC, id DESC LIMIT 1")
    ResumeScore selectLatestByUserIdAndResumeId(@Param("userId") Long userId, @Param("resumeId") Long resumeId);
}
