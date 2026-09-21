# API Contract

## 1. 目标

这份文档是当前项目的最终可落地版接口契约，目标有两个：

1. 和当前前端页面、`stores`、`mocks` 的字段尽量直接对齐，减少前端映射成本。
2. 支持先用固定 `mock` 输出，后续真实后端接入后保持同样返回结构，不改页面结构。

当前前端项目结构已按业务域拆分完成：

- `home`
- `jd`
- `resume`
- `interview`
- `profile`

当前前端强依赖的核心字段命名：

- JD：`title`、`summary`、`tags`、`hardSkills`、`softSkills`
- 简历优化：`score`、`total`、`badges`、`sections`
- 面试：`jobTitle`、`statusText`、`chatList[].role`、`chatList[].content`
- 简历库：`defaultResume`、`history`

## 2. 统一约定

### 2.1 接口前缀

```text
/api/app
```

### 2.2 成功返回

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

### 2.3 分页返回

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 2.4 错误返回

```json
{
  "code": 500,
  "msg": "解析失败，请稍后重试",
  "data": null
}
```

### 2.5 时间格式

统一使用 ISO 8601：

```text
2026-03-19T12:00:00+08:00
```

### 2.6 上传类接口

统一使用：

```text
Content-Type: multipart/form-data
```

### 2.7 枚举约定

#### 通用状态

- `pending`
- `processing`
- `success`
- `failed`

#### 面试状态

- `ongoing`
- `finished`
- `aborted`

#### 消息角色

为兼容当前前端，统一使用：

- `user`
- `agent`

#### 简历来源类型

- `original`
- `optimized`

## 3. Mock 阶段建议

当前阶段完全可以先走固定 `mock` 返回值，推荐做法：

1. 上传接口只校验表单并返回固定 `resumeId`。
2. JD 解析详情、简历优化详情、面试回复、面试报告都返回固定结构。
3. 所有固定返回值的字段名必须和本文档一致。
4. 后续后端接入时，只替换数据来源，不改响应结构。

## 4. 接口清单

---

## 4.1 首页 Home

### 接口 1：获取首页摘要信息

```http
GET /api/app/home/summary
```

请求参数：无

返回字段说明：

- `assistantCard`：当前首页卡片直接使用
- `userInfo`：后续首页或个人中心可复用
- `latestResume`：后续首页摘要展示
- `latestJdAnalysis`：后续首页摘要展示
- `latestInterview`：后续首页摘要展示

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "assistantCard": {
      "tag": "沉浸式对练模式",
      "title": "面试助手",
      "subtitle": "AGENT",
      "actionLabel": "开始模拟面试"
    },
    "userInfo": {
      "userId": "u1001",
      "nickname": "张三",
      "avatarUrl": "https://xxx/avatar.png"
    },
    "latestResume": {
      "resumeId": "r1001",
      "fileName": "前端开发工程师_最新版.pdf",
      "updatedAt": "2026-03-19T12:00:00+08:00",
      "score": 86
    },
    "latestJdAnalysis": {
      "analysisId": "j1001",
      "title": "前端开发工程师",
      "updatedAt": "2026-03-19T11:30:00+08:00"
    },
    "latestInterview": {
      "sessionId": "s1001",
      "jobTitle": "前端开发工程师",
      "status": "finished",
      "updatedAt": "2026-03-19T10:20:00+08:00"
    }
  }
}
```

当前前端直接消费字段：

- `assistantCard.tag`
- `assistantCard.title`
- `assistantCard.subtitle`
- `assistantCard.actionLabel`

---

## 4.2 JD 输入页

### 接口 2：提交 JD 解析

```http
POST /api/app/jd/analyze
```

请求体：

```json
{
  "jdText": "岗位职责......任职要求......"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "analysisId": "j1001",
    "title": "前端开发工程师",
    "status": "success"
  }
}
```

说明：

- 当前前端后续建议在 `stores/jd` 中保存 `analysisId`
- `title` 用于快速预览或后续跳转

---

## 4.3 JD 解析结果页

### 接口 3：获取 JD 解析详情

```http
GET /api/app/jd/{analysisId}
```

路径参数：

- `analysisId`：JD 解析记录 ID

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "analysisId": "j1001",
    "title": "前端开发工程师",
    "summary": "该岗位要求候选人具备扎实的前端基础、工程化能力和跨团队协作能力。",
    "tags": ["性能优化", "组件化", "团队协作", "工程化"],
    "hardSkills": ["Vue", "TypeScript", "Webpack", "前端性能优化"],
    "softSkills": ["沟通表达", "问题分析", "团队协作"],
    "interviewFocus": ["项目亮点", "性能优化经验", "组件设计能力"],
    "recommendedQuestions": [
      "请介绍一个你做过的前端性能优化案例",
      "你是如何做组件封装的？"
    ],
    "createdAt": "2026-03-19T11:30:00+08:00"
  }
}
```

当前前端直接消费字段：

- `title`
- `summary`
- `tags`
- `hardSkills`
- `softSkills`

---

## 4.4 简历上传页

### 接口 4：上传简历并触发解析/优化

```http
POST /api/app/resume/upload
```

请求类型：

```text
multipart/form-data
```

表单参数：

- `file`：简历文件，必填
- `jdText`：可选
- `analysisId`：可选，已存在的 JD 解析 ID

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "resumeId": "r1001",
    "fileName": "前端开发工程师_原始简历.pdf",
    "parseStatus": "success",
    "optimizeStatus": "success"
  }
}
```

说明：

- Mock 阶段可以不真正解析文件内容，只返回固定 `resumeId`
- 当前前端后续建议在 `stores/resume` 中增加 `resumeId`、`fileName`

---

## 4.5 AI 简历优化页

### 接口 5：获取简历优化详情

```http
GET /api/app/resume/{resumeId}/detail
```

路径参数：

- `resumeId`：简历 ID

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "resumeId": "r1001",
    "fileName": "前端开发工程师_原始简历.pdf",
    "score": 82,
    "total": 100,
    "badges": ["排版良好", "JD匹配度：78%"],
    "finalActionLabel": "生成最终简历",
    "sections": [
      {
        "id": "work-experience",
        "title": "工作经历",
        "status": "待优化",
        "originalText": "负责前端页面开发和接口联调。",
        "suggestions": [
          "建议增加技术栈细节",
          "建议增加性能优化成果"
        ],
        "actionLabel": "一键智能重写",
        "optimizedText": "",
        "primaryActionLabel": "应用修改",
        "secondaryActionLabel": "撤销还原",
        "applied": false
      },
      {
        "id": "project-experience",
        "title": "项目经历",
        "status": "已优化",
        "originalText": "负责活动页开发。",
        "suggestions": [
          "建议补充量化结果"
        ],
        "actionLabel": "一键智能重写",
        "optimizedText": "负责基于 Vue3 + TypeScript 的业务页面开发与接口联调，通过组件化重构提升页面复用率，并完成首屏性能优化。",
        "primaryActionLabel": "应用修改",
        "secondaryActionLabel": "撤销还原",
        "applied": false
      },
      {
        "id": "education",
        "title": "教育背景",
        "status": "待优化",
        "originalText": "",
        "suggestions": [],
        "actionLabel": "一键智能重写",
        "optimizedText": "",
        "primaryActionLabel": "应用修改",
        "secondaryActionLabel": "撤销还原",
        "applied": false
      }
    ]
  }
}
```

当前前端直接消费字段：

- `score`
- `total`
- `badges`
- `finalActionLabel`
- `sections[].id`
- `sections[].title`
- `sections[].status`
- `sections[].originalText`
- `sections[].suggestions`
- `sections[].actionLabel`
- `sections[].optimizedText`
- `sections[].primaryActionLabel`
- `sections[].secondaryActionLabel`

### 接口 6：单个模块一键重写

```http
POST /api/app/resume/{resumeId}/rewrite-section
```

请求体：

```json
{
  "sectionId": "work-experience",
  "instruction": "更偏前端开发岗，突出性能优化"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "work-experience",
    "status": "已优化",
    "optimizedText": "负责基于 Vue3 + TypeScript 的业务页面开发与组件抽象，结合懒加载与资源压缩优化首屏加载时间。",
    "suggestions": [
      "增加项目规模描述会更好",
      "可补充数据指标"
    ],
    "primaryActionLabel": "应用修改",
    "secondaryActionLabel": "撤销还原"
  }
}
```

### 接口 7：应用某个模块的优化结果

```http
POST /api/app/resume/{resumeId}/apply-section
```

请求体：

```json
{
  "sectionId": "work-experience"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "work-experience",
    "applied": true,
    "status": "已优化"
  }
}
```

### 接口 8：生成最终简历版本

```http
POST /api/app/resume/{resumeId}/generate-final
```

请求体：

```json
{
  "fileName": "前端开发工程师_优化版.pdf"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "resumeId": "r1002",
    "sourceResumeId": "r1001",
    "fileName": "前端开发工程师_优化版.pdf",
    "versionNo": "v2",
    "sourceType": "optimized",
    "downloadUrl": "/api/app/resume/r1002/download"
  }
}
```

---

## 4.6 我的简历库页

### 接口 9：获取简历列表

```http
GET /api/app/resume/list
```

查询参数：

- `keyword`：可选
- `sourceType`：可选，`original / optimized`
- `pageNum`
- `pageSize`

返回说明：

- `defaultResume` 和 `history`：为了兼容当前前端
- `list`：为了支持后续真正分页管理页

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "defaultResume": {
      "id": "r1002",
      "name": "前端开发工程师_优化版.pdf",
      "sizeInMb": 2.4,
      "updatedAt": "2026-03-19T12:10:00+08:00",
      "actionLabel": "继续润色",
      "isDefault": true,
      "versionNo": "v2",
      "sourceType": "optimized"
    },
    "history": [
      {
        "id": "r1002",
        "name": "前端开发工程师_优化版.pdf",
        "sizeInMb": 2.4,
        "updatedAt": "2026-03-19T12:10:00+08:00",
        "variant": "pink",
        "isDefault": true,
        "versionNo": "v2",
        "sourceType": "optimized"
      },
      {
        "id": "r1001",
        "name": "前端开发工程师_原始简历.pdf",
        "sizeInMb": 2.1,
        "updatedAt": "2026-03-19T11:40:00+08:00",
        "variant": "blue",
        "isDefault": false,
        "versionNo": "v1",
        "sourceType": "original"
      }
    ],
    "list": [
      {
        "id": "r1002",
        "name": "前端开发工程师_优化版.pdf",
        "sizeInMb": 2.4,
        "updatedAt": "2026-03-19T12:10:00+08:00",
        "isDefault": true,
        "versionNo": "v2",
        "sourceType": "optimized"
      },
      {
        "id": "r1001",
        "name": "前端开发工程师_原始简历.pdf",
        "sizeInMb": 2.1,
        "updatedAt": "2026-03-19T11:40:00+08:00",
        "isDefault": false,
        "versionNo": "v1",
        "sourceType": "original"
      }
    ],
    "total": 2,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

当前前端直接消费字段：

- `defaultResume`
- `history`

### 接口 10：设为默认简历

```http
POST /api/app/resume/{resumeId}/set-default
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "resumeId": "r1002",
    "isDefault": true
  }
}
```

### 接口 11：删除简历

```http
DELETE /api/app/resume/{resumeId}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "resumeId": "r1001",
    "deleted": true
  }
}
```

### 接口 12：下载简历

```http
GET /api/app/resume/{resumeId}/download
```

推荐方案：

- 生产环境直接返回文件流
- 前端调试或 H5 场景可返回下载链接

返回链接示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "resumeId": "r1002",
    "downloadUrl": "https://xxx/download/r1002.pdf"
  }
}
```

---

## 4.7 模拟面试入口页

### 接口 13：开始模拟面试

```http
POST /api/app/interview/start
```

请求体：

```json
{
  "jobTitle": "前端开发工程师",
  "resumeId": "r1002",
  "jdAnalysisId": "j1001"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": "s1001",
    "jobTitle": "前端开发工程师",
    "statusText": "模拟面试进行中",
    "openingMessage": {
      "id": "m1",
      "role": "agent",
      "type": "text",
      "content": "你好，请先做一个简短的自我介绍。",
      "audioUrl": "https://xxx/audio/opening.mp3",
      "createdAt": "2026-03-19T13:00:00+08:00"
    }
  }
}
```

当前前端直接消费字段：

- `jobTitle`
- `statusText`
- `openingMessage.role`
- `openingMessage.content`

---

## 4.8 模拟面试会话页

### 接口 14：获取面试历史消息

```http
GET /api/app/interview/{sessionId}/history
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": "s1001",
    "jobTitle": "前端开发工程师",
    "statusText": "模拟面试进行中",
    "messages": [
      {
        "id": "m1",
        "role": "agent",
        "type": "text",
        "content": "你好，请先做一个简短的自我介绍。",
        "audioUrl": "https://xxx/audio/m1.mp3",
        "createdAt": "2026-03-19T13:00:00+08:00"
      }
    ]
  }
}
```

### 接口 15：上传一轮音频并获取 AI 回复

```http
POST /api/app/interview/{sessionId}/audio-turn
```

请求类型：

```text
multipart/form-data
```

表单参数：

- `audioFile`：录音文件
- `durationMs`：可选

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": "s1001",
    "userMessage": {
      "id": "m2",
      "role": "user",
      "type": "text",
      "content": "大家好，我有三年前端开发经验，主要做 Vue 项目开发。",
      "createdAt": "2026-03-19T13:01:00+08:00"
    },
    "agentMessage": {
      "id": "m3",
      "role": "agent",
      "type": "text",
      "content": "好的，请介绍一个你参与过的最有代表性的项目，并说明你负责的部分。",
      "audioUrl": "https://xxx/audio/reply_1.mp3",
      "createdAt": "2026-03-19T13:01:03+08:00"
    },
    "transcriptText": "大家好，我有三年前端开发经验，主要做 Vue 项目开发。",
    "needFollowUp": true,
    "interviewState": "ongoing"
  }
}
```

说明：

- 为兼容当前前端，返回 `userMessage` 和 `agentMessage` 两个消息对象
- 不建议前端传 `turnNo`，由后端自行维护轮次

### 接口 16：文本模式提交一轮回答

```http
POST /api/app/interview/{sessionId}/text-turn
```

请求体：

```json
{
  "text": "我参与了一个校园二手交易平台项目，负责前端页面开发和接口联调。"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": "s1001",
    "userMessage": {
      "id": "m4",
      "role": "user",
      "type": "text",
      "content": "我参与了一个校园二手交易平台项目，负责前端页面开发和接口联调。",
      "createdAt": "2026-03-19T13:02:00+08:00"
    },
    "agentMessage": {
      "id": "m5",
      "role": "agent",
      "type": "text",
      "content": "你在这个项目中遇到过什么性能问题？你是怎么优化的？",
      "audioUrl": "https://xxx/audio/reply_2.mp3",
      "createdAt": "2026-03-19T13:02:02+08:00"
    },
    "needFollowUp": true,
    "interviewState": "ongoing"
  }
}
```

### 接口 17：结束面试

```http
POST /api/app/interview/{sessionId}/finish
```

请求体：

```json
{
  "reason": "user_finish"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": "s1001",
    "finished": true,
    "reportReady": true
  }
}
```

---

## 4.9 面试结果页

### 接口 18：获取面试报告

```http
GET /api/app/interview/{sessionId}/report
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": "s1001",
    "totalScore": 84,
    "transcriptSummary": "整体表达流畅，项目经历较完整，但在量化成果和性能优化细节方面还可加强。",
    "dimensions": [
      { "name": "表达能力", "score": 85 },
      { "name": "岗位匹配度", "score": 88 },
      { "name": "逻辑能力", "score": 82 },
      { "name": "技术深度", "score": 80 }
    ],
    "strengths": [
      "表达较自然，沟通清晰",
      "项目经历与目标岗位较相关"
    ],
    "weaknesses": [
      "部分回答缺少量化结果",
      "技术细节展开不够充分"
    ],
    "suggestions": [
      "回答项目题时增加具体指标",
      "复习前端性能优化常见方案"
    ]
  }
}
```

说明：

- 当前项目暂未实现面试报告页
- 这个接口可以先保留并提供固定 mock

---

## 4.10 个人中心页

### 接口 19：获取用户个人资料

```http
GET /api/app/user/profile
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": "u1001",
    "nickname": "张三",
    "avatarUrl": "https://xxx/avatar.png",
    "phone": "138****0000",
    "email": "test@example.com",
    "stats": {
      "resumeCount": 5,
      "jdAnalysisCount": 3,
      "interviewCount": 6
    }
  }
}
```

---

## 5. 推荐前端 API 文件拆分

```text
src/api/
  home.js
  jd.js
  resume.js
  interview.js
  user.js
```

建议函数：

### `src/api/home.js`

- `getHomeSummary()`

### `src/api/jd.js`

- `analyzeJd(data)`
- `getJdDetail(analysisId)`

### `src/api/resume.js`

- `uploadResume(formData)`
- `getResumeDetail(resumeId)`
- `rewriteResumeSection(resumeId, data)`
- `applyResumeSection(resumeId, data)`
- `generateFinalResume(resumeId, data)`
- `getResumeList(params)`
- `setDefaultResume(resumeId)`
- `deleteResume(resumeId)`
- `downloadResume(resumeId)`

### `src/api/interview.js`

- `startInterview(data)`
- `getInterviewHistory(sessionId)`
- `submitAudioTurn(sessionId, formData)`
- `submitTextTurn(sessionId, data)`
- `finishInterview(sessionId, data)`
- `getInterviewReport(sessionId)`

### `src/api/user.js`

- `getUserProfile()`

## 6. 对当前前端的最小影响

这份接口文档不会推翻当前项目结构，但后续接入时建议补这些状态字段：

### `stores/jd`

- `analysisId`

### `stores/resume`

- `resumeId`
- `fileName`
- `uploadStatus`
- `optimizeStatus`

### `stores/interview`

- `sessionId`
- `historyLoaded`
- `report`

### 新增建议

- `src/api/user.js`
- `src/mocks/user.js`
- 后续新增面试报告页

## 7. 实施顺序建议

如果按“先 mock、后真实后端”推进，推荐顺序：

1. `JD analyze + JD detail`
2. `resume upload + resume detail`
3. `resume section rewrite/apply/generate`
4. `resume list/set-default/delete/download`
5. `interview start + history + text-turn`
6. `interview audio-turn + finish + report`
7. `home summary + user profile`

这样最贴合你当前项目页面推进节奏。
