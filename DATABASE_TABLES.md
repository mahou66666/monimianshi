# 数据库表梳理

基于导出文件：`interview_agent.sql`

这份文档按业务模块梳理数据库表，方便快速定位“这张表是干什么的、现在有没有用上”。

## 1. 用户与账号

### `user_info`
- 作用：用户主表，存登录账号本体。
- 典型字段：`id`、`phone`、`password`、`status`、`nickname`、`last_login_time`
- 当前用途：用户管理、登录、状态启停、基础用户列表。

### `user_detail`
- 作用：用户扩展信息表，存更完整的个人资料。
- 典型字段：真实姓名、性别、毕业年份、微信号、头像、期望薪资、MBTI 等。
- 当前用途：用户详情展示、求职者档案补充。

### `verification_code_record`
- 作用：验证码记录表。
- 当前用途：偏预留能力，常见于短信验证码、邮箱验证码、找回密码。

### `account_token_session`
- 作用：登录会话表，存 token 摘要、状态、过期时间、撤销时间。
- 当前用途：更完整的会话管理设计。
- 备注：表已建，但你们当前后台主链路更多还是 Redis/JWT 在跑。

## 2. 权限与后台管理

### `permission`
- 作用：权限定义表。
- 典型内容：权限名称、权限码、权限描述。
- 当前用途：权限管理、按钮级权限、菜单可见性控制。

### `user_permission_rel`
- 作用：用户和权限的关联表。
- 当前用途：给用户分配权限，决定某个用户能看什么、能点什么。

## 3. 公司与 JD

### `company`
- 作用：公司表。
- 典型字段：`company_code`、`company_name`、`company_location`、`industry`、`scale`
- 当前用途：公司基础数据，JD 和投递链路的支撑表。

### `jd_job`
- 作用：JD 主表。
- 典型内容：岗位名称、岗位类型、城市、薪资、岗位描述、岗位要求。
- 当前用途：上传 JD、JD 列表、JD 管理。

### `jd_guide`
- 作用：JD 指导建议表。
- 典型内容：一条 JD 对应一份“岗位解读 / 面试准备建议”。
- 当前用途：JD 指导建议页面。
- 备注：表里现在混有两类数据：
  - 早期英文模板文本
  - 后期 JSON 结构文本

### `user_history_jd`
- 作用：用户历史 JD 记录表。
- 当前用途：记录某个用户导入/生成/查看过哪些 JD。

### `user_history_company`
- 作用：用户历史公司记录表。
- 当前用途：记录用户和公司维度的历史行为。
- 备注：当前数据量看起来不大，更像预留或未充分使用。

## 4. 简历主链路

### `resume`
- 作用：简历主表。
- 当前用途：简历业务主记录，通常关联用户、状态、标题等基础信息。

### `resume_file`
- 作用：简历文件表。
- 典型内容：文件名、存储路径、所属用户、上传时间。
- 当前用途：你们现在“简历存本地，数据库存地址”主要落在这张表。

### `resume_content`
- 作用：简历结构化内容表。
- 当前用途：存算法解析后的主要文本结果。
- 备注：`resume_cut_docker` 解析完成后，核心结果会写回这里。

### `resume_fragment`
- 作用：简历片段表。
- 当前用途：把简历进一步拆成教育、项目、实习等片段，方便后续按模块处理。

### `resume_delivery_rel`
- 作用：简历投递关系表。
- 当前用途：表示“哪份简历投给了哪个公司 / 哪个岗位”。

## 5. 简历智能分析层

### `resume_problem`
- 作用：简历题目表。
- 当前用途：存基于简历生成的问题或题目。
- 备注：数据库里已经有数据，但当前前后端还没把这条链路完整接起来。

### `resume_score`
- 作用：简历评分表。
- 典型内容：总分、多维度分、总结、建议、问题列表。
- 当前用途：数据库层已经具备评分结果承载能力。
- 备注：表里已有真实数据，但当前后台接口/页面闭环还没完全打通。

### `resume_core_competitive_index`
- 作用：简历核心竞争力指标表。
- 当前用途：更偏分析层，适合做“竞争力画像/量化分析”。

### `resume_ai_operation`
- 作用：简历 AI 操作记录表。
- 当前用途：记录某份简历被执行过哪些 AI/算法操作。

## 6. 用户履历拆分档案

### `user_education`
- 作用：教育经历表。
- 典型内容：学校、专业、学历、起止时间、描述。
- 当前用途：教育经历标准化存储。

### `user_project`
- 作用：项目经历表。
- 典型内容：项目名称、项目时间、职责、描述、成果。
- 当前用途：项目经历标准化存储。

### `user_internship`
- 作用：实习经历表。
- 典型内容：公司、岗位、时间、职责、成果。
- 当前用途：实习经历标准化存储。

### `user_certificate`
- 作用：证书/奖项表。
- 当前用途：作为简历档案补充信息。

## 7. 现在最值得关注的 8 张表

如果只抓你当前后台主线，先记这 8 张就够了：

1. `user_info`
2. `permission`
3. `user_permission_rel`
4. `resume`
5. `resume_file`
6. `resume_content`
7. `jd_job`
8. `jd_guide`

## 8. 当前项目和这些表的关系

### 已经明显用起来的
- `user_info`
- `permission`
- `user_permission_rel`
- `resume`
- `resume_file`
- `resume_content`
- `jd_job`
- `jd_guide`

### 数据库里有，但前后端还没完全接完的
- `resume_problem`
- `resume_score`
- `resume_core_competitive_index`
- `resume_ai_operation`
- `user_internship`
- `user_certificate`

### 更像预留能力或后续扩展的
- `verification_code_record`
- `account_token_session`
- `user_history_company`

## 9. 一句话总结

这套库的核心骨架是：

- 用户：`user_info`
- 权限：`permission` + `user_permission_rel`
- 简历：`resume` + `resume_file` + `resume_content`
- JD：`jd_job` + `jd_guide`
- 智能分析扩展：`resume_problem` + `resume_score`

如果后面你要继续推进功能，优先围绕这几组表看就行，不会迷路。
