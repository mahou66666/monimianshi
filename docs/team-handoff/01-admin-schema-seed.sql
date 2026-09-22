-- Fresh local database only. No existing rows are exported. Abort on any SQL error.
SET NAMES utf8mb4;
CREATE DATABASE interview_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE interview_agent;
CREATE TABLE `account_token_session`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'user_info.id',
  `token_digest` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHA-256 hash of access token',
  `refresh_token_digest` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHA-256 hash of refresh token',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '1=active,2=revoked,3=expired',
  `expires_at` datetime NOT NULL COMMENT 'Session expiry time',
  `revoked_at` datetime NULL DEFAULT NULL COMMENT 'Revoked time',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_token_digest`(`token_digest`) USING BTREE,
  INDEX `idx_session_user_status`(`user_id`, `status`) USING BTREE,
  INDEX `idx_session_expires_at`(`expires_at`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Account token sessions' ROW_FORMAT = Dynamic;


CREATE TABLE `company`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公司ID（主键）',
  `company_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公司编码（唯一标识，如ALI/ByteDance）',
  `company_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公司名称（如阿里巴巴（中国）有限公司）',
  `company_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公司地点/工作地点',
  `industry` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '所属行业（如互联网/金融/教育）',
  `scale` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '公司规模（如500-1000人/10000人以上）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1-有效 2-无效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_company_code`(`company_code`) USING BTREE COMMENT '公司编码唯一',
  INDEX `idx_company_location`(`company_location`) USING BTREE,
  INDEX `idx_company_name`(`company_name`) USING BTREE COMMENT '按公司名称查询',
  INDEX `idx_industry`(`industry`) USING BTREE COMMENT '按行业筛选'
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历投递公司表' ROW_FORMAT = Dynamic;


CREATE TABLE `jd_guide`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `jd_id` bigint(20) NOT NULL COMMENT '关联 jd_job.id',
  `guide_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'JD解析与通用准备建议（无历史版本）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_jd_guide_jd_id`(`jd_id`) USING BTREE,
  INDEX `idx_jd_guide_update_time`(`update_time`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'JD指导建议表' ROW_FORMAT = Dynamic;


CREATE TABLE `jd_job`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'JD岗位ID（主键）',
  `job_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码（唯一标识，如ALI-Java-001）',
  `job_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称（如Java开发工程师/产品经理）',
  `company_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联company.id（所属公司）',
  `jd_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'jd岗位描述',
  `jd_corereq` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'jd岗位核心要求',
  `job_type` tinyint(4) NULL DEFAULT 1 COMMENT '岗位类型：1-校招 2-社招 3-实习',
  `salary_range` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '薪资范围（如15k-25k/月）',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '工作城市（如北京/上海-浦东新区）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1-有效 2-无效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_job_code`(`job_code`) USING BTREE COMMENT '岗位编码唯一',
  INDEX `idx_city_job_type`(`city`, `job_type`) USING BTREE COMMENT '按城市+岗位类型筛选',
  INDEX `idx_company_job`(`company_id`, `job_name`) USING BTREE COMMENT '按公司+岗位查询'
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历JD工作岗位表' ROW_FORMAT = Dynamic;


CREATE TABLE `permission`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `perm_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `perm_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `perm_desc` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `menu_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_perm_code`(`perm_code`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `resume`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '简历ID（主键）',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联user.id（所属用户）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '简历标题（如“高级产品经理--B端方向”）',
  `num` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简历版本号',
  `progress` tinyint(4) NULL DEFAULT NULL COMMENT '完成进度（0-100）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行业分类（如“互联网”“金融”）',
  `tags` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '标签（逗号分隔，如“产品,中文”）',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '中文' COMMENT '简历语言（如“中文”“English”）',
  `content_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '内容类型：1-富文本 2-Word文件 3-Excel文件',
  `index_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '竞争维度名称（如“参数1”“参数2”）',
  `score` tinyint(4) NOT NULL DEFAULT 0 COMMENT '维度评分（0-100）',
  `compete_ratio` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '竞争力比例（相对于其他简历的百分比，0.00-100.00）',
  `is_default` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否默认简历：1-是 0-否',
  `parent_resume_id` bigint(20) NULL DEFAULT NULL COMMENT '父简历ID（为空=原始上传；非空=由该简历派生的版本）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_resume_parent_resume_id`(`parent_resume_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE COMMENT '按用户查询简历'
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历基础信息表' ROW_FORMAT = Dynamic;


CREATE TABLE `resume_ai_operation`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '操作ID（主键）',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联user.id',
  `resume_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联resume.id（若为新生成则为NULL）',
  `ai_base_id` bigint(20) NOT NULL COMMENT '关联ai_base_info.id',
  `ai_resume_id` bigint(20) NOT NULL COMMENT '关联简历分段id',
  `operation_type` tinyint(4) NOT NULL COMMENT '操作类型：1-AI生成简历 2-AI修改简历 3-AI分析问题',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'AI输入的提示词（如“生成产品经理校招简历”）',
  `result` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作结果（如“生成成功”“修改完成”）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_resume`(`user_id`, `resume_id`) USING BTREE COMMENT '按用户+简历查询操作记录'
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI简历操作记录表' ROW_FORMAT = Dynamic;


CREATE TABLE `resume_content`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内容ID（主键）',
  `resume_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联resume.id',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '简历内容（富文本/HTML）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_resume_id`(`resume_id`) USING BTREE COMMENT '一份简历对应一份内容'
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历内容详情表' ROW_FORMAT = Dynamic;


CREATE TABLE `resume_core_competitive_index`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `education_score` int(11) NULL DEFAULT NULL COMMENT '教育背景评分',
  `work_score` int(11) NULL DEFAULT NULL COMMENT '工作经历评分',
  `project_score` int(11) NULL DEFAULT NULL COMMENT '项目经验评分',
  `skill_score` int(11) NULL DEFAULT NULL COMMENT '专业技能评分',
  `award_score` int(11) NULL DEFAULT NULL COMMENT '奖项荣誉评分',
  `job_fit_score` int(11) NULL DEFAULT NULL COMMENT '岗位匹配评分',
  `avg_score` int(11) NULL DEFAULT NULL COMMENT '平均分',
  `max_score` int(11) NULL DEFAULT NULL COMMENT '最高分',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `advantage_item` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '优势项（来自问题分析highlights第一个）',
  `competitiveness_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '综合竞争力等级：较低/中等/良好/优秀',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历核心竞争指数' ROW_FORMAT = Dynamic;


CREATE TABLE `resume_delivery_rel`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '投递记录ID（主键）',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联user.id（投递用户）',
  `resume_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联resume.id（投递的简历）',
  `company_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联company.id（投递的公司）',
  `jd_job_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联jd_job.id（投递的JD岗位）',
  `delivery_count` int(11) NOT NULL DEFAULT 1 COMMENT '该简历投递该岗位的次数（默认1次，重复投递自增）',
  `first_delivery_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次投递时间',
  `latest_delivery_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最新投递时间',
  `delivery_status` tinyint(4) NULL DEFAULT 1 COMMENT '投递状态：1-已投递 2-已查看 3-面试邀请 4-已拒绝 5-已录用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '投递备注（如“内推/官网投递”）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_resume_company_job`(`user_id`, `resume_id`, `company_id`, `jd_job_id`) USING BTREE,
  INDEX `fk_delivery_company`(`company_id`) USING BTREE,
  INDEX `fk_delivery_job`(`jd_job_id`) USING BTREE,
  INDEX `idx_delivery_status`(`delivery_status`) USING BTREE COMMENT '按投递状态筛选',
  INDEX `idx_latest_delivery_time`(`latest_delivery_time`) USING BTREE COMMENT '按最新投递时间排序',
  INDEX `idx_resume_job`(`resume_id`, `jd_job_id`) USING BTREE,
  INDEX `idx_user_company`(`user_id`, `company_id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历投递关系表（含投递次数统计）' ROW_FORMAT = Dynamic;


CREATE TABLE `resume_file`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID（主键）',
  `resume_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联resume.id',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始文件名（如“我的简历.docx”）',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件类型（如application/msword、application/vnd.openxmlformats-officedocument.wordprocessingml.document）',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小（字节）',
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件存储路径（如/minio/resume/2025/05/10/13800138000_123.docx）',
  `download_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件下载链接（可带签名，有效期）',
  `preview_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简历预览图URL（用于列表/详情页展示）',
  `md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件MD5值（防重复上传）',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_md5`(`md5`) USING BTREE COMMENT 'MD5唯一，避免重复上传',
  UNIQUE INDEX `uk_resume_id`(`resume_id`) USING BTREE COMMENT '一份简历仅对应一个文件'
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历文件存储表' ROW_FORMAT = Dynamic;


CREATE TABLE `resume_fragment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '片段ID',
  `user_id` bigint(20) NOT NULL COMMENT '关联 user.id',
  `resume_id` bigint(20) NOT NULL COMMENT '关联 resume.id',
  `section_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '片段类型',
  `item_index` int(11) NOT NULL DEFAULT 0 COMMENT '该片段下的条目序号',
  `fragment_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '片段原文',
  `fragment_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '片段内容Hash',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_resume_fragment_u_r_s`(`user_id`, `resume_id`, `section_key`) USING BTREE,
  INDEX `idx_resume_fragment_u_r`(`user_id`, `resume_id`) USING BTREE,
  INDEX `idx_resume_fragment_u_r_s`(`user_id`, `resume_id`, `section_key`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历片段表' ROW_FORMAT = Dynamic;


CREATE TABLE `resume_problem`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '问题ID（主键）',
  `resume_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联resume.id',
  `problem_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题标题（如“缺乏项目经验”）',
  `problem_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '问题描述（如“岗位匹配度低”）',
  `priority` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '优先级',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '建议',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '问题状态：1-待解决 2-已解决',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_resume_id`(`resume_id`) USING BTREE COMMENT '按简历查询问题'
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历待优化问题表' ROW_FORMAT = Dynamic;


CREATE TABLE `resume_score`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评分记录ID',
  `resume_id` bigint(20) NOT NULL COMMENT '关联 resume.id',
  `user_id` bigint(20) NOT NULL COMMENT '关联 user.id',
  `education_score` int(11) NOT NULL COMMENT '教育经历评分(0-100)',
  `work_score` int(11) NOT NULL COMMENT '工作/实习经历评分(0-100)',
  `project_score` int(11) NOT NULL COMMENT '项目经历评分(0-100)',
  `skill_score` int(11) NOT NULL COMMENT '技能评分(0-100)',
  `award_score` int(11) NOT NULL COMMENT '获奖与证书评分(0-100)',
  `job_fit_score` int(11) NULL DEFAULT NULL COMMENT '岗位匹配度评分(0-100)',
  `total_score` int(11) NOT NULL COMMENT '总分(0-100)',
  `dimension_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '打分理由/维度分析',
  `advice` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修改建议',
  `problems_json` json NOT NULL COMMENT '问题列表(JSON数组，元素为字符串)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_resume_score_resume_id`(`resume_id`) USING BTREE,
  INDEX `idx_resume_score_resume_time`(`resume_id`, `create_time`) USING BTREE,
  INDEX `idx_resume_score_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简历评分表' ROW_FORMAT = Dynamic;


CREATE TABLE `user_certificate`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `cert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `issue_org` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `issue_date` date NULL DEFAULT NULL,
  `cert_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `user_detail`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '详情ID（主键）',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联user.id',
  `school_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '毕业学校(关联t_school.id)',
  `target_JD_id` bigint(20) NULL DEFAULT NULL COMMENT '目标JD(关联jd_job.id)',
  `target_company_id` bigint(20) NULL DEFAULT NULL COMMENT '目标公司(关联company.id)',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '真实姓名',
  `gender` tinyint(4) NULL DEFAULT 0 COMMENT '性别：1-男 2-女 0-未知',
  `identity` tinyint(4) NOT NULL DEFAULT 0 COMMENT '我的身份 0-未选择 1-学生 2-职场人',
  `graduation_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '毕业年份（如“2027年”）',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '身份证号（脱敏存储）',
  `wechat` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '微信号',
  `qq` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'QQ号',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '头像链接',
  `estimated_salary` decimal(10, 2) NULL DEFAULT NULL COMMENT '预估薪资（元/月，系统计算）',
  `target_salary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '目标薪资（元/月，用户自填）',
  `mbti_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户MBTI类型（如INTJ/ESFP）',
  `prepare_score` tinyint(4) NOT NULL DEFAULT 0 COMMENT '备战准备度（如65分）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id`) USING BTREE COMMENT '一个用户对应一条详情'
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户详细信息表' ROW_FORMAT = Dynamic;


CREATE TABLE `user_education`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `school_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `degree` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `start_time` date NOT NULL,
  `end_time` date NULL DEFAULT NULL,
  `intro` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `user_history_company`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `company_id` bigint(20) NULL DEFAULT NULL,
  `company_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_deleted` tinyint(4) NOT NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `user_history_jd`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `jd_id` bigint(20) NULL DEFAULT NULL,
  `jd_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `is_deleted` tinyint(4) NOT NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `user_info`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号（11位或带注销后缀如13812345678-时间戳）',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '加密后的密码（建议用BCrypt）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常 2-禁用',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名称',
  `is_deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '删除状态：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_active_time` datetime NULL DEFAULT NULL COMMENT '最近活跃时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone`) USING BTREE COMMENT '手机号唯一',
  INDEX `idx_user_info_last_active_time`(`last_active_time`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户基础信息表' ROW_FORMAT = Dynamic;


CREATE TABLE `user_internship`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `company` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `start_time` date NOT NULL,
  `end_time` date NULL DEFAULT NULL,
  `intro` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `user_permission_rel`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `perm_id` bigint(20) UNSIGNED NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_perm`(`user_id`, `perm_id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `user_project`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `start_time` date NOT NULL,
  `end_time` date NULL DEFAULT NULL,
  `intro` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `verification_code_record`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Receiver phone',
  `scene` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'login/register/reset',
  `code_digest` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHA-256 hash of verification code',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0=pending,1=used,2=expired',
  `verify_attempts` int(11) NOT NULL DEFAULT 0 COMMENT 'Verification attempts',
  `expires_at` datetime NOT NULL COMMENT 'Code expiry time',
  `used_at` datetime NULL DEFAULT NULL COMMENT 'Used time',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_code_phone_scene_status`(`phone`, `scene`, `status`) USING BTREE,
  INDEX `idx_code_expires_at`(`expires_at`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Verification code records' ROW_FORMAT = Dynamic;

INSERT INTO user_info(id,phone,password,status,user_name,is_deleted) VALUES (1001,'138-0000-0303','$2a$10$Cmd.pUFEJfk.uopXneC7JOvrYZV1Z0gLCtct/3AcMJcYzsghoU8Ni',1,'本地测试管理员',0);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (1,'job:add','添加岗位','/job/add',1);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (2,'company:view','查看公司','/company/list',1);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (3,'resume:manage','管理简历','',1);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (4,'job:view','查看岗位','',1);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (5,'job:edit','编辑岗位','',1);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (6,'company:manage','用户与公司管理','',1);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (7,'resume:view','查看简历','',1);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (8,'resume:evaluate','简历评估','',1);
INSERT INTO permission(id,perm_code,perm_name,menu_path,status) VALUES (9,'system:log','系统日志','',1);
INSERT INTO user_permission_rel(user_id,perm_id) SELECT 1001,id FROM permission WHERE status=1;
