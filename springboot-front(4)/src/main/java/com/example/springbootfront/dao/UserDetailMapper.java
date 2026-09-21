package com.example.springbootfront.dao;

import com.example.springbootfront.entity.UserDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserDetailMapper {

    @Results(id = "userDetailResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "schoolId", column = "school_id"),
            @Result(property = "targetJdId", column = "target_JD_id"),
            @Result(property = "targetCompanyId", column = "target_company_id"),
            @Result(property = "realName", column = "real_name"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "identity", column = "identity"),
            @Result(property = "graduationYear", column = "graduation_year"),
            @Result(property = "idCard", column = "id_card"),
            @Result(property = "wechat", column = "wechat"),
            @Result(property = "qq", column = "qq"),
            @Result(property = "birthday", column = "birthday"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "estimatedSalary", column = "estimated_salary"),
            @Result(property = "targetSalary", column = "target_salary"),
            @Result(property = "mbtiType", column = "mbti_type"),
            @Result(property = "prepareScore", column = "prepare_score"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    @Select("""
            select id, user_id, school_id, target_JD_id, target_company_id, real_name, gender, identity,
                   graduation_year, id_card, wechat, qq, birthday, avatar_url, estimated_salary,
                   target_salary, mbti_type, prepare_score, create_time, update_time
            from user_detail
            where id = #{id}
            """)
    UserDetail selectById(Long id);

    @ResultMap("userDetailResultMap")
    @Select("""
            select id, user_id, school_id, target_JD_id, target_company_id, real_name, gender, identity,
                   graduation_year, id_card, wechat, qq, birthday, avatar_url, estimated_salary,
                   target_salary, mbti_type, prepare_score, create_time, update_time
            from user_detail
            where user_id = #{userId}
            """)
    UserDetail selectByUserId(Long userId);

    @ResultMap("userDetailResultMap")
    @Select("""
            select id, user_id, school_id, target_JD_id, target_company_id, real_name, gender, identity,
                   graduation_year, id_card, wechat, qq, birthday, avatar_url, estimated_salary,
                   target_salary, mbti_type, prepare_score, create_time, update_time
            from user_detail
            order by id desc
            """)
    List<UserDetail> selectAll();

    @Insert("""
            insert into user_detail (
                user_id, school_id, target_JD_id, target_company_id, real_name, gender, identity,
                graduation_year, id_card, wechat, qq, birthday, avatar_url, estimated_salary,
                target_salary, mbti_type, prepare_score
            ) values (
                #{userId}, #{schoolId}, #{targetJdId}, #{targetCompanyId}, #{realName}, #{gender}, #{identity},
                #{graduationYear}, #{idCard}, #{wechat}, #{qq}, #{birthday}, #{avatarUrl}, #{estimatedSalary},
                #{targetSalary}, #{mbtiType}, #{prepareScore}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserDetail userDetail);

    @Update("""
            update user_detail
            set user_id = #{userId},
                school_id = #{schoolId},
                target_JD_id = #{targetJdId},
                target_company_id = #{targetCompanyId},
                real_name = #{realName},
                gender = #{gender},
                identity = #{identity},
                graduation_year = #{graduationYear},
                id_card = #{idCard},
                wechat = #{wechat},
                qq = #{qq},
                birthday = #{birthday},
                avatar_url = #{avatarUrl},
                estimated_salary = #{estimatedSalary},
                target_salary = #{targetSalary},
                mbti_type = #{mbtiType},
                prepare_score = #{prepareScore}
            where id = #{id}
            """)
    int updateById(UserDetail userDetail);

    @Delete("delete from user_detail where id = #{id}")
    int deleteById(Long id);
}
