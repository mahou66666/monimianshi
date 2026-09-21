package com.a05.admin.mapper;

import com.a05.admin.entity.ResumeFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResumeFileMapper extends BaseMapper<ResumeFile> {

    @Select("SELECT COUNT(1) FROM resume_file rf INNER JOIN resume r ON rf.resume_id = r.id WHERE r.user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT r.user_id AS userId, COUNT(1) AS resumeCount",
            "FROM resume_file rf INNER JOIN resume r ON rf.resume_id = r.id",
            "WHERE r.user_id IN",
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>",
            "#{userId}",
            "</foreach>",
            "GROUP BY r.user_id",
            "</script>"
    })
    List<Map<String, Object>> countByUserIds(@Param("userIds") List<Long> userIds);

    @Select("SELECT * FROM resume_file WHERE md5 = #{md5} LIMIT 1")
    ResumeFile selectByMd5(@Param("md5") String md5);

    @Select("SELECT * FROM resume_file WHERE resume_id = #{resumeId} LIMIT 1")
    ResumeFile selectFirstByResumeId(@Param("resumeId") Long resumeId);
}
