import java.sql.*;
import java.time.LocalDateTime;

public class SeedVideoDemo {
    private static final String URL = "jdbc:mysql://106.54.162.6:3306/interview_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String USER = "zzh";
    private static final String PASSWORD = "zzh";

    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);
            try {
                seedUsers(conn);
                seedResumes(conn);
                seedJds(conn);
                seedResumeProblems(conn);
                seedResumeScores(conn);
                conn.commit();
                System.out.println("[seed-video-demo] done");
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private static void seedUsers(Connection conn) throws Exception {
        updateUser(conn, 1014L, "Stan（超级管理员）", "13000000001", 1, LocalDateTime.now().minusMinutes(8));
        updateUser(conn, 1010L, "陈以宁（企业管理员）", "17959181494", 1, LocalDateTime.now().minusMinutes(20));
        updateUser(conn, 1011L, "许嘉禾（简历顾问）", "16378833085", 1, LocalDateTime.now().minusMinutes(35));
        updateUser(conn, 1013L, "宋时予（前端候选人）", "13936700169", 1, LocalDateTime.now().minusHours(1));
        updateUser(conn, 1012L, "顾明川（后端候选人）", "13904070013", 1, LocalDateTime.now().minusHours(2));
        updateUser(conn, 1009L, "周砚秋（数据分析候选人）", "13963005579", 1, LocalDateTime.now().minusHours(3));
        updateUser(conn, 1008L, "林知夏（校园招聘HR）", "13933224043", 1, LocalDateTime.now().minusHours(5));
        updateUser(conn, 1001L, "康浩泽（产品经理）", "15395440138", 1, LocalDateTime.now().minusHours(7));
        updateUser(conn, 1007L, "林远舟（演示账号）", "13900000002", 1, LocalDateTime.now().minusHours(9));
    }

    private static void seedResumes(Connection conn) throws Exception {
        updateResume(conn, 62L, 1001L, "康浩泽_后台产品经理_优化版", LocalDateTime.now().minusMinutes(16));
        updateResume(conn, 57L, 1001L, "康浩泽_产品助理_项目型简历", LocalDateTime.now().minusMinutes(22));
        updateResume(conn, 31L, 1009L, "周砚秋_数据分析实习简历", LocalDateTime.now().minusMinutes(28));
        updateResume(conn, 59L, 1014L, "Stan_Java后端工程师_展示简历A", LocalDateTime.now().minusMinutes(34));
        updateResume(conn, 58L, 1014L, "Stan_前端实习生_展示简历B", LocalDateTime.now().minusMinutes(40));
        updateResume(conn, 61L, 1007L, "林远舟_运营策划简历_演示版", LocalDateTime.now().minusMinutes(46));
        updateResume(conn, 60L, 1007L, "林远舟_算法实习简历_演示版", LocalDateTime.now().minusMinutes(52));
    }

    private static void seedJds(Connection conn) throws Exception {
        long jd1 = upsertJd(conn,
                "DEMO-VIDEO-BE-20260414",
                "研发效能平台后端工程师",
                1L,
                "负责研发效能平台、权限体系与审批流系统建设，推动内部平台的稳定性与可观测性提升。",
                "熟悉 Java / Spring Boot / MySQL / Redis，具备中后台系统设计经验，能独立推进复杂模块落地。",
                2,
                "30k-45k",
                "上海",
                LocalDateTime.now().minusDays(1).minusHours(2));
        upsertGuide(conn, jd1,
                "岗位理解：偏平台型后端岗位，强调权限、流程与系统稳定性。\n准备建议：突出你做过的后台系统、复杂表结构设计、接口性能优化与故障排查经验。\n面试提醒：把一个你亲自负责的模块讲透，包括业务背景、方案取舍、上线结果和复盘。",
                LocalDateTime.now().minusDays(1).minusHours(1));

        long jd2 = upsertJd(conn,
                "DEMO-VIDEO-PM-20260414",
                "AI 产品经理",
                1L,
                "面向校招与社招场景设计智能简历、JD 指导建议和评分流程，推动模型能力落地到业务闭环。",
                "有 B 端产品经验，理解推荐、搜索或智能体类产品逻辑，具备良好的跨团队沟通与方案拆解能力。",
                2,
                "25k-38k",
                "北京",
                LocalDateTime.now().minusDays(2).minusHours(3));
        upsertGuide(conn, jd2,
                "岗位理解：偏业务理解和流程设计，核心是把抽象能力转成真实可用的产品体验。\n准备建议：重点讲清楚需求拆解、方案权衡、数据指标和推动落地的过程。\n面试提醒：举一个你如何在不确定需求下推动上线的真实例子。",
                LocalDateTime.now().minusDays(2).minusHours(2));

        long jd3 = upsertJd(conn,
                "DEMO-VIDEO-FE-20260414",
                "前端实习生",
                1L,
                "参与管理后台和求职者端页面开发，负责列表页、表单流和图表模块的交互优化。",
                "熟悉 Vue 或 React，掌握组件拆分、接口联调和基础工程化，愿意打磨细节体验。",
                3,
                "220-300/天",
                "深圳",
                LocalDateTime.now().minusDays(3).minusHours(4));
        upsertGuide(conn, jd3,
                "岗位理解：更看重基础扎实和页面落地能力。\n准备建议：准备一个完整页面案例，能讲清组件划分、状态管理和接口联调。\n面试提醒：把你做过的一个后台页面从需求到上线讲完整。",
                LocalDateTime.now().minusDays(3).minusHours(3));

        long jd4 = upsertJd(conn,
                "DEMO-VIDEO-DATA-20260414",
                "数据分析师",
                1L,
                "负责用户行为分析、漏斗诊断和活动复盘，支持平台运营决策与策略迭代。",
                "熟悉 SQL、Excel、数据可视化工具，有 A/B Test 或增长分析经验更佳。",
                2,
                "18k-28k",
                "杭州",
                LocalDateTime.now().minusDays(4).minusHours(5));
        upsertGuide(conn, jd4,
                "岗位理解：需要从数据发现问题，再形成行动建议。\n准备建议：多讲指标体系、分析链路、异常定位和最终业务结果。\n面试提醒：避免只说工具，重点说你如何通过分析改变业务动作。",
                LocalDateTime.now().minusDays(4).minusHours(4));

        long jd5 = upsertJd(conn,
                "DEMO-VIDEO-JAVA-CAMPUS-20260414",
                "校招 Java 工程师",
                1L,
                "参与内容平台服务端开发，负责基础接口、缓存设计和线上问题排查。",
                "计算机基础扎实，熟悉 Java、Spring、MySQL，具备良好的代码习惯和学习能力。",
                1,
                "15k-22k",
                "广州",
                LocalDateTime.now().minusDays(5).minusHours(6));
        upsertGuide(conn, jd5,
                "岗位理解：偏基础能力和成长潜力。\n准备建议：把课程项目、实习经历和你做过的后端练习题串成一条线。\n面试提醒：准备数据库索引、事务、缓存和并发相关的基础问题。",
                LocalDateTime.now().minusDays(5).minusHours(5));
    }

    private static void seedResumeProblems(Connection conn) throws Exception {
        upsertResumeProblem(conn, 59L, "项目成果量化不足", "项目经历描述完整，但缺少明确结果指标，展示力度不够。", "medium", "为每个项目补充 1-2 个量化结果，例如性能提升、效率提升或业务转化指标。", LocalDateTime.now().minusHours(6));
        upsertResumeProblem(conn, 58L, "技能栈描述偏散", "技能点较多但层级不清，重点方向不突出。", "medium", "按前端框架、工程化、协作工具三组整理技能，突出主技术栈。", LocalDateTime.now().minusHours(7));
        upsertResumeProblem(conn, 62L, "业务价值表达偏弱", "经历写了做什么，但没有说清楚为什么重要。", "high", "增加业务背景、目标和上线效果，让经历从任务导向升级为结果导向。", LocalDateTime.now().minusHours(8));
        upsertResumeProblem(conn, 57L, "教育信息结构不统一", "教育经历格式不统一，时间、学校和专业信息不够工整。", "low", "统一为 学校 / 学历 / 时间 的格式，保证一眼可扫读。", LocalDateTime.now().minusHours(9));
        upsertResumeProblem(conn, 31L, "岗位方向不够聚焦", "当前经历覆盖面较广，但和数据分析岗位的匹配标签不够集中。", "medium", "强化 SQL、可视化、指标分析等关键词，减少无关描述。", LocalDateTime.now().minusHours(10));
    }

    private static void seedResumeScores(Connection conn) throws Exception {
        upsertResumeScore(conn, 59L, 1014L, 88, 76, 90, 85, 72, 84,
                "教育背景完整，项目经历表达清晰，技术栈和岗位方向匹配度较高。",
                "建议继续补充项目结果指标，并把一段最能体现架构设计能力的经历放在前面。",
                "[{\"title\":\"补充业务指标\",\"level\":\"medium\"}]",
                LocalDateTime.now().minusHours(4));
        upsertResumeScore(conn, 58L, 1014L, 84, 68, 78, 89, 65, 80,
                "前端技能较扎实，工程化能力是亮点，项目经历还可以再往结果导向收束。",
                "建议把最有代表性的页面或系统经历写成 STAR 结构，突出复杂交互和性能优化。",
                "[{\"title\":\"强化结果导向\",\"level\":\"medium\"}]",
                LocalDateTime.now().minusHours(5));
        upsertResumeScore(conn, 62L, 1001L, 86, 80, 84, 76, 70, 83,
                "整体结构成熟，产品经历丰富，对业务和协作的表达较完整。",
                "建议增加更硬的结果指标，例如转化率、留存率、效率提升比例。",
                "[{\"title\":\"增加量化结果\",\"level\":\"high\"}]",
                LocalDateTime.now().minusHours(6));
        upsertResumeScore(conn, 57L, 1001L, 82, 72, 79, 70, 60, 76,
                "基础信息完整，项目经历可读性尚可，但亮点密度偏低。",
                "建议把最强项目提前，并删除和目标岗位关系较弱的描述。",
                "[{\"title\":\"突出核心项目\",\"level\":\"medium\"}]",
                LocalDateTime.now().minusHours(7));
        upsertResumeScore(conn, 31L, 1009L, 80, 74, 77, 82, 61, 78,
                "分析能力相关关键词较齐全，和数据分析方向有一定匹配度。",
                "建议补充完整的数据分析闭环案例，包括问题定义、分析方法和业务结论。",
                "[{\"title\":\"补强分析闭环\",\"level\":\"medium\"}]",
                LocalDateTime.now().minusHours(8));
    }

    private static void updateUser(Connection conn, long id, String userName, String phone, int status, LocalDateTime lastActiveTime) throws Exception {
        String sql = "UPDATE user_info SET user_name = ?, phone = ?, status = ?, update_time = ?, last_active_time = ? WHERE id = ? AND is_deleted = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userName);
            ps.setString(2, phone);
            ps.setInt(3, status);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(5, Timestamp.valueOf(lastActiveTime));
            ps.setLong(6, id);
            ps.executeUpdate();
        }
    }

    private static void updateResume(Connection conn, long id, long userId, String title, LocalDateTime updateTime) throws Exception {
        String sql = "UPDATE resume SET user_id = ?, title = ?, update_time = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setTimestamp(3, Timestamp.valueOf(updateTime));
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    private static long upsertJd(Connection conn, String jobCode, String jobName, long companyId,
                                 String desc, String req, int jobType, String salary,
                                 String city, LocalDateTime time) throws Exception {
        Long existingId = findOneLong(conn, "SELECT id FROM jd_job WHERE job_code = ? LIMIT 1", jobCode);
        if (existingId == null) {
            String insertSql = "INSERT INTO jd_job (job_code, job_name, company_id, jd_descriptions, jd_corereq, job_type, salary_range, city, status, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, jobCode);
                ps.setString(2, jobName);
                ps.setLong(3, companyId);
                ps.setString(4, desc);
                ps.setString(5, req);
                ps.setInt(6, jobType);
                ps.setString(7, salary);
                ps.setString(8, city);
                ps.setTimestamp(9, Timestamp.valueOf(time));
                ps.setTimestamp(10, Timestamp.valueOf(time));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
            throw new IllegalStateException("Failed to insert jd_job: " + jobCode);
        }

        String updateSql = "UPDATE jd_job SET job_name = ?, company_id = ?, jd_descriptions = ?, jd_corereq = ?, job_type = ?, salary_range = ?, city = ?, status = 1, update_time = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, jobName);
            ps.setLong(2, companyId);
            ps.setString(3, desc);
            ps.setString(4, req);
            ps.setInt(5, jobType);
            ps.setString(6, salary);
            ps.setString(7, city);
            ps.setTimestamp(8, Timestamp.valueOf(time));
            ps.setLong(9, existingId);
            ps.executeUpdate();
        }
        return existingId;
    }

    private static void upsertGuide(Connection conn, long jdId, String guideText, LocalDateTime time) throws Exception {
        Long existingId = findOneLong(conn, "SELECT id FROM jd_guide WHERE jd_id = ? LIMIT 1", jdId);
        if (existingId == null) {
            String insertSql = "INSERT INTO jd_guide (jd_id, guide_text, create_time, update_time) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setLong(1, jdId);
                ps.setString(2, guideText);
                ps.setTimestamp(3, Timestamp.valueOf(time));
                ps.setTimestamp(4, Timestamp.valueOf(time));
                ps.executeUpdate();
            }
            return;
        }

        String updateSql = "UPDATE jd_guide SET guide_text = ?, update_time = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, guideText);
            ps.setTimestamp(2, Timestamp.valueOf(time));
            ps.setLong(3, existingId);
            ps.executeUpdate();
        }
    }

    private static void upsertResumeProblem(Connection conn, long resumeId, String title, String desc,
                                            String priority, String suggestion, LocalDateTime time) throws Exception {
        Long existingId = findOneLong(conn, "SELECT id FROM resume_problem WHERE resume_id = ? AND problem_title = ? LIMIT 1", resumeId, title);
        if (existingId == null) {
            String insertSql = "INSERT INTO resume_problem (resume_id, problem_title, problem_desc, priority, suggestion, status, create_time, update_time) VALUES (?, ?, ?, ?, ?, 1, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setLong(1, resumeId);
                ps.setString(2, title);
                ps.setString(3, desc);
                ps.setString(4, priority);
                ps.setString(5, suggestion);
                ps.setTimestamp(6, Timestamp.valueOf(time));
                ps.setTimestamp(7, Timestamp.valueOf(time));
                ps.executeUpdate();
            }
            return;
        }

        String updateSql = "UPDATE resume_problem SET problem_desc = ?, priority = ?, suggestion = ?, status = 1, update_time = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, desc);
            ps.setString(2, priority);
            ps.setString(3, suggestion);
            ps.setTimestamp(4, Timestamp.valueOf(time));
            ps.setLong(5, existingId);
            ps.executeUpdate();
        }
    }

    private static void upsertResumeScore(Connection conn, long resumeId, long userId,
                                          int educationScore, int workScore, int projectScore,
                                          int skillScore, int awardScore, int totalScore,
                                          String dimensionAnalysis, String advice, String problemsJson,
                                          LocalDateTime time) throws Exception {
        Long existingId = findOneLong(conn, "SELECT id FROM resume_score WHERE resume_id = ? ORDER BY id DESC LIMIT 1", resumeId);
        int jobFitScore = Math.max(60, Math.min(95, (projectScore + skillScore + workScore) / 3));
        if (existingId == null) {
            String insertSql = "INSERT INTO resume_score (resume_id, user_id, education_score, work_score, project_score, skill_score, award_score, job_fit_score, total_score, dimension_analysis, advice, problems_json, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                bindResumeScore(ps, resumeId, userId, educationScore, workScore, projectScore, skillScore, awardScore, jobFitScore, totalScore, dimensionAnalysis, advice, problemsJson, time, time, 1);
                ps.executeUpdate();
            }
            return;
        }

        String updateSql = "UPDATE resume_score SET user_id = ?, education_score = ?, work_score = ?, project_score = ?, skill_score = ?, award_score = ?, job_fit_score = ?, total_score = ?, dimension_analysis = ?, advice = ?, problems_json = ?, update_time = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            int idx = 1;
            ps.setLong(idx++, userId);
            ps.setInt(idx++, educationScore);
            ps.setInt(idx++, workScore);
            ps.setInt(idx++, projectScore);
            ps.setInt(idx++, skillScore);
            ps.setInt(idx++, awardScore);
            ps.setInt(idx++, jobFitScore);
            ps.setInt(idx++, totalScore);
            ps.setString(idx++, dimensionAnalysis);
            ps.setString(idx++, advice);
            ps.setString(idx++, problemsJson);
            ps.setTimestamp(idx++, Timestamp.valueOf(time));
            ps.setLong(idx, existingId);
            ps.executeUpdate();
        }
    }

    private static void bindResumeScore(PreparedStatement ps, long resumeId, long userId,
                                        int educationScore, int workScore, int projectScore,
                                        int skillScore, int awardScore, int jobFitScore, int totalScore,
                                        String dimensionAnalysis, String advice, String problemsJson,
                                        LocalDateTime createTime, LocalDateTime updateTime,
                                        int startIndex) throws Exception {
        int idx = startIndex;
        ps.setLong(idx++, resumeId);
        ps.setLong(idx++, userId);
        ps.setInt(idx++, educationScore);
        ps.setInt(idx++, workScore);
        ps.setInt(idx++, projectScore);
        ps.setInt(idx++, skillScore);
        ps.setInt(idx++, awardScore);
        ps.setInt(idx++, jobFitScore);
        ps.setInt(idx++, totalScore);
        ps.setString(idx++, dimensionAnalysis);
        ps.setString(idx++, advice);
        ps.setString(idx++, problemsJson);
        ps.setTimestamp(idx++, Timestamp.valueOf(createTime));
        ps.setTimestamp(idx, Timestamp.valueOf(updateTime));
    }

    private static Long findOneLong(Connection conn, String sql, Object... args) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long value = rs.getLong(1);
                    return rs.wasNull() ? null : value;
                }
                return null;
            }
        }
    }
}