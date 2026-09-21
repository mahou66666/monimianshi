package com.a05.admin.mapper;

import com.a05.admin.entity.JdGuide;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JdGuideMapper extends BaseMapper<JdGuide> {

    @Select("SELECT * FROM jd_guide WHERE jd_id = #{jdId} LIMIT 1")
    JdGuide selectByJdId(@Param("jdId") Long jdId);

    @Delete("DELETE FROM jd_guide WHERE jd_id = #{jdId}")
    int deleteByJdId(@Param("jdId") Long jdId);
}
