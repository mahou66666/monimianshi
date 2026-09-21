package com.a05.admin.mapper;

import com.a05.admin.entity.ResumeContent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResumeContentMapper extends BaseMapper<ResumeContent> {

    @Select("SELECT * FROM resume_content WHERE resume_id = #{resumeId} LIMIT 1")
    ResumeContent selectByResumeId(@Param("resumeId") Long resumeId);
}

