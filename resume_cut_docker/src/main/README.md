简历解析算法模块 (Resume Parser Module)

本项目封装了一套基于 LLM (DeepSeek) 的智能简历解析算法。采用了 "核心服务共享，多端适配接入" 的设计架构，确保前台（Web API）和后台（本地/定时任务）均能高效、统一地调用解析能力。

1. 核心架构说明
   核心解析逻辑被封装在 ResumeParserService 中，输入为标准 InputStream，输出为强类型 ResumeResultDTO。这使得算法与文件来源解耦：

> [!IMPORTANT]
> **核心架构差异**
> * 🔵 **前台 Web**：通过 `ResumeController` 接收 HTTP `MultipartFile` 流。
> * 🟠 **后台 Task**：通过 `ResumeBackendManager` 读取本地磁盘 `File` 流。

代码段
graph TD
User(前端用户) -->|HTTP Upload| Controller(ResumeController)
Admin(后台系统/DB) -->|File Path| Manager(ResumeBackendManager)

    Controller -->|InputStream| Service(ResumeParserService)
    Manager -->|InputStream| Service
    
    Service -->|Text Extraction| POI(PDFBox/POI)
    Service -->|AI Analysis| LLM(DeepSeek API)
    
    LLM --> Service
    Service -->|ResumeResultDTO| Controller
    Service -->|ResumeResultDTO| Manager
2. 配置文件
   在使用前，请确保 application.properties 或 application.yml 中配置了 LLM 相关参数：

Properties
# LLM 配置
llm.api.url=https://api.siliconflow.cn/v1/chat/completions
llm.api.key=YOUR_API_KEY_HERE
llm.model=deepseek-ai/DeepSeek-V3
3. 前台调用说明 (Frontend / Web API)
   适用于 Web 页面、小程序或 H5 端，通过 HTTP 协议进行文件上传。

Base URL: /api/resume

3.1 单文件解析

上传单个简历文件，实时返回解析结果。

接口地址: POST /parse

Content-Type: multipart/form-data

参数:

file: 文件对象 (支持 .pdf, .doc, .docx)

返回: JSON 对象 (ResumeResultDTO)

调用示例 (cURL):

Bash
curl -X POST -F "file=@/path/to/resume.pdf" http://localhost:8080/api/resume/parse
3.2 批量解析 (Web 端)

用户一次性选中多个文件进行上传解析。

接口地址: POST /parse/batch

Content-Type: multipart/form-data

参数:

files: 文件对象数组

返回: JSON 数组 (List<ResumeResultDTO>)

4. 后台调用说明 (Backend / Internal)
   适用于定时任务、后台管理系统、文件服务器扫描等场景。不通过 HTTP，而是通过 Spring 依赖注入直接调用 Java Bean，效率更高。

核心组件: ResumeBackendManager

4.1 注入组件

在您的 Service 或 Job 类中注入管理器：

Java
@Autowired
private ResumeBackendManager resumeBackendManager;
4.2 功能接口列表

A. 处理单体本地文件

根据绝对路径解析单个文件。

Java
/**
* @param filePath 本地文件绝对路径 (e.g., "/data/upload/resume.pdf")
* @return ResumeResultDTO 解析结果
  */
  ResumeResultDTO result = resumeBackendManager.processSingleFile("/data/resume.pdf");
  B. 处理文件路径列表 (批量)

如果您从数据库查询出一批文件路径，可以使用此接口批量处理。系统会自动过滤不存在或格式不支持的文件。

Java
List<String> paths = Arrays.asList(
"/data/resumes/user_a.pdf",
"/data/resumes/user_b.docx"
);

/**
* @param filePaths 文件路径列表
* @return List<ResumeResultDTO> 解析结果列表
  */
  List<ResumeResultDTO> results = resumeBackendManager.processFilePaths(paths);
  C. 扫描并处理整个目录

自动扫描指定文件夹下的所有支持文件（.pdf, .doc, .docx）并进行批量解析。

Java
/**
* @param directoryPath 文件夹路径
* @return List<ResumeResultDTO> 解析结果列表
  */
  List<ResumeResultDTO> results = resumeBackendManager.processDirectory("/data/daily_upload_pool/");
5. 数据结构说明 (Output)
   无论前台还是后台，解析成功后返回的数据结构 (ResumeResultDTO) 统一如下：

字段名 (JSON Key)	类型	说明
BASIC_INFO	Object	基本信息 (含 name, phone, email, age, university 等)
SKILLS	String	技能特长 (纯文本)
EDUCATION	List	教育经历 (每段经历为一个字符串)
WORK_EXPERIENCE	List	工作经历 (每段经历为一个字符串)
PROJECT_EXPERIENCE	List	项目经历 (每段经历为一个字符串)
INTERNSHIP_EXPERIENCE	List	实习经历 (每段经历为一个字符串)
AWARDS	String	荣誉奖项
SELF_EVALUATION	String	自我评价
errorMessage	String	如果解析失败，此字段包含错误原因
rawContent	String	(可选) 解析失败时保留的原始文本
JSON 响应示例

JSON
{
"BASIC_INFO": {
"name": "张三",
"phone": "13800138000",
"email": "zhangsan@example.com",
"university": "某某大学",
"degree": "本科",
"job_intention": "Java后端开发"
},
"SKILLS": "熟练掌握 Java, Spring Boot, MySQL...",
"EDUCATION": [
"2018-2022 某某大学 计算机科学与技术"
],
"WORK_EXPERIENCE": [
"2022-至今 某科技公司 后端开发工程师..."
],
"errorMessage": null
}
6. 注意事项
   文件格式: 目前仅支持 .pdf, .doc, .docx。其他格式在后台调用时会被自动过滤，前台调用会返回 400 错误。

异常处理: 批量处理时（无论是 Web 还是 Backend），单个文件的失败不会中断整个批次。失败的文件会在结果列表中以 errorMessage 字段非空的形式返回。

性能建议: AI 解析接口响应时间较长（通常 3-10秒/个）。对于前台 Web 批量上传，建议限制单次上传数量（如 10 个以内），或建议改为异步上传模式。后台调用无此限制。