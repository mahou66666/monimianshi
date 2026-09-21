package com.a05.admin.mapper;

import com.a05.admin.entity.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    // 简单写一条带未删除标记的自定义查询语句，方便 Service 层调用
    @Select("SELECT * FROM user_info WHERE phone = #{phone} AND is_deleted = 0 LIMIT 1")
    UserInfo selectByPhone(@org.apache.ibatis.annotations.Param("phone") String phone);
}
