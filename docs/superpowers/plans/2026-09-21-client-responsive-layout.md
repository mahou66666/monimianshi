# 前台客户端自动响应式改造 Implementation Plan

> **For agentic workers:** 使用 superpowers:executing-plans 按任务实施。步骤使用复选框跟踪；执行状态以复选框及同目录验收记录为准。

**Goal:** 让候选人前台随浏览器窗口宽度自动调整布局，兼容窄屏、平板、桌面和浏览器缩放，保留现有配色、卡片和人物风格。

**Architecture:** 复用现有 Vue 页面和组件，以 CSS Flex/Grid 和媒体查询调整布局。移除桌面固定手机外壳，明确各页面的滚动容器；宽度变化不重新挂载业务组件。

**Tech Stack:** 项目现有 Vue 3、Vue Router、Vite、CSS、ECharts；不新增 UI 框架或升级依赖。

**Spec:** 本文件的“范围与布局约定”依据用户本轮明确要求：先自动响应式，保留风格，先外壳与首页，再表单和列表，最后面试与报告。

## 范围与布局约定

- 仅候选人端 `vue-front (4)/`；不修改管理后台、后端接口、数据库、认证规则或业务流程。
- 本期不增加手机模拟器、手动布局切换、整体等比缩放或新业务功能。
- 建议初始断点：小屏 <768px；中屏 768–1199px；大屏 ≥1200px。断点按内容拥挤情况微调，不按设备名称检测。
- 默认页面内容最大宽度 1200px；登录类表单最大宽度 480px；账户设置类单列表单最大宽度 720px。面试页独立处理可用空间。
- 保留现有背景色、品牌色、圆角卡片、图标、人物素材和动画；只调整尺寸、留白、排列及必要的结构容器。
- 单个页面不同时创建手机版和桌面版业务组件；不得以宽度设置 RouterView 的 key 或触发路由跳转。
- 表单/列表页保持内容可滚动；面试页优先让聊天区独立滚动，极短窗口时提供页面滚动兜底。
- 当前项目根目录的 Git 检查返回“不是 Git 仓库”。实施前确认前台是否独立受版本控制；若没有，只备份本次待改文件，不初始化仓库或覆盖已有备份。
- 2026-09-21 执行更新：任务 1–5 布局实现及阶段检查完成，任务 6 自动化与构建通过；未完整覆盖的状态/键盘测试、真实浏览器缩放仍保留未勾选。硬件与服务边界见验收记录，不将部分通过表述为全面通过。

## 验收重点

1. 低高度窗口和 200% 浏览器缩放：提交、关闭、录音和发送按钮可达，不能被 overflow:hidden 永久裁切。
2. 长文件名、长 JD、长消息、空数据及错误提示：正常换行或局部滚动，不撑宽整个页面。
3. 跨断点缩放时：表单草稿、已选文件、上传进度和面试状态保留，不增加重复请求。
4. 手机键盘和安全区：聚焦输入后能看到输入框及操作按钮，底栏不遮挡内容。
5. 弹窗及图表：弹窗适配可用高度，图表随自身容器变化更新，不只依赖浏览器窗口 resize。

## 任务 1：建立响应式页面外壳

**文件：** 修改 `vue-front (4)/src/App.vue`。检查 `src/main.js` 的样式加载顺序，不默认新增全局样式文件。

**产出：** 所有路由可使用完整可用视口；导航和页面共享明确的高度关系。

- [x] 实施前记录 390×844、768×1024、1440×900 下首页及登录页的现状截图，确认页面可以访问。
- [x] 删除 App.vue 中 ≥500px 时强制 390×844、手机边框及居中固定高度的规则；保留路由背景变量和事件监听。
- [x] 使用下述外壳基础规则替换对应旧规则，页面宽度上限放在页面内容层，不限制背景层：

```css
html, body { margin: 0; padding: 0; }
#app { width: 100%; min-height: 100dvh; }
#app-wrapper {
  width: 100%; height: 100dvh;
  display: flex; flex-direction: column;
  position: relative;
  background: var(--shell-background, #eaddd3);
}
.main-content {
  flex: 1; min-width: 0; min-height: 0;
  display: flex; flex-direction: column;
  overflow: auto;
}
```

- [x] 对现有页面逐一检查滚动归属：普通页面允许内容增长；已有独立滚动列表和聊天区的父级使用 min-height:0，避免形成重复滚动条。
- [x] 验证 499px/500px 不再突然切成手机框；在 320px 宽及 600px 高窗口下确认无不可达按钮。记录其他页面暴露的问题，纳入所属任务。

## 任务 2：首页与导航适配

**文件：** `src/views/home/HomeView.vue`、`src/views/home/components/FeatureGrid.vue`、`src/views/home/components/AssistantCard.vue`、`src/components/common/TabBar.vue`、`src/views/upload/UploadPlaceholderView.vue`，必要时调整 `src/App.vue` 的导航布局规则。

**依赖：** 任务 1 的自适应外壳；保留 TabBar 的 currentTab 属性和 change 事件。

- [x] 小屏保留现有上下排列和底部导航，取消导致内容被压扁的强制高度；人物区域可缩小但不能遮挡开始按钮。
- [x] 中屏适当增大间距；大屏首页改为功能入口与面试助手左右排列。以现有两个子组件为网格项，不复制内容。
- [x] 大屏布局可使用以下规则作为起点，卡片本身延续现有风格：

```css
.home-container { width: 100%; max-width: 1200px; margin-inline: auto; }
@media (min-width: 1200px) {
  .home-container {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
    gap: 24px;
  }
}
```

- [x] ≥768px 将同一个 TabBar 排到主内容上方，改为紧凑横向导航，增加首页、上传、我的文字标签；保留原路由的 showTabBar 控制，不扩展导航到原先隐藏它的页面。
- [x] 底栏增加 env(safe-area-inset-bottom) 安全区；导航不因主内容增长而被压缩。
- [x] 验证三处首页入口、开始面试和三个导航入口，确保路由行为与改造前一致；在 320px、768px、1200px、1440px 下检查卡片、人物和按钮完整显示。

## 任务 3：认证表单与个人中心适配

**文件：** `src/views/account/components/AuthShell.vue`、`src/styles/login-auth-overrides.css`、`src/views/account/LoginView.vue`、`RegisterView.vue`、`ResetPasswordView.vue`、`AccountSecurityView.vue`；`src/views/profile/ProfileView.vue`、`UserInfoView.vue`、`SettingsView.vue`。

**依赖：** 任务 1 的滚动规则；保留所有现有认证和保存处理函数。

- [x] AuthShell 大屏表单居中、最大宽度 480px；短窗口允许内容滚动，顶部和底部操作均可达。
- [x] 检查全局 login-auth-overrides.css 与组件局部样式的优先级，避免添加相互冲突的补丁；验证码行保留输入空间，小屏必要时换行。
- [x] 个人中心宽屏对信息和功能卡片分栏，编辑资料及设置保留最大 720px 的易读单列。
- [x] 输入布局采用 minmax(0,1fr)、min-width:0 和允许长文本换行；仅在内容溢出时调整现有固定宽度。
- [ ] 检查登录、注册、重置密码、账号安全、个人中心、个人信息、设置七页的常规和错误提示状态。
- [ ] 输入草稿后连续跨断点缩放，确认值保留；200% 缩放和手机键盘弹出时，表单及按钮可见或能滚动到达。不为布局测试发送真实短信或重置账号密码。

## 任务 4：简历、JD 与历史列表适配

**文件：** `src/views/resume/ResumeUploadView.vue`、`ResumeLibraryView.vue`、`components/ResumePageHeader.vue`；`src/views/jd/JdInputView.vue`、`components/JdPageHeader.vue`；`src/views/profile/InterviewHistoryView.vue`、`JdHistoryView.vue`。

**依赖：** 任务 1 的内容宽度与滚动约定；保持列表顺序、筛选、上传和预览状态逻辑。

- [x] 简历上传大屏将文件区和 JD 输入区分栏，窄屏恢复纵向；结构化摘要从固定五列改为按可用宽度换行。
- [x] 简历库和历史记录在宽屏采用适量多列或限宽列表，保持时间顺序；工具栏和卡片操作区允许换行，不新增复杂表格。
- [x] JD 输入区域避免固定最小高度占满低高度窗口，提交按钮仍在正常滚动流中。
- [x] 简历预览弹窗桌面扩大至最大 1000px，窄屏全屏；标题栏/工具栏固定在弹窗内部，正文独立滚动。文档自身的预览缩放保持现有语义，不影响应用布局。
- [ ] 对长文件名、长 JD、空列表、加载失败、历史记录详情弹窗检查水平溢出和关闭按钮可达性。
- [x] 已选择文件、已输入 JD 或设置筛选后缩放，确认状态不变；用已有测试文件验证预览。如需真实上传或 AI 调用，限定一份测试数据并记录副作用及结果。

## 任务 5：模拟面试与分析报告适配

**文件：** `src/views/interview/InterviewView.vue`、`components/InterviewChat.vue`、`components/InterviewControls.vue`、`components/InterviewHeader.vue`、`InterviewSummaryView.vue`；`src/views/resume/AiResumeResultView.vue`、`src/views/jd/JdResultView.vue`。

**依赖：** 前面已稳定的外壳；保留 interviewStore 和页面生命周期的业务语义。

- [x] 面试小屏保留纵向结构；宽屏用 CSS 网格将人物和聊天分栏，输入/录音操作区位于聊天区下方。使用同一套组件，不通过 v-if 按屏幕尺寸切换业务树。
- [x] 人物区从固定 220px 高改为随可用空间变化；短屏优先保证聊天、转写确认、发送、结束面试和错误重试可用。
- [x] 保留聊天自动滚动行为，长消息正常换行；输入框增高时聊天仍可滚动，必要时页面整体滚动兜底。
- [x] 简历结果、JD 结果、面试总结大屏将摘要/图表与详细分析分栏，手机顺序保持不变，长报告可完整阅读。
- [x] 面试总结 ECharts 对实际图表容器增加 ResizeObserver，并在 onBeforeUnmount 中 disconnect。保留现有 chartInstance.resize()，避免 resize 触发新的业务请求。
- [ ] 验证图表在跨断点后无裁切；加载、错误、空报告、完整报告均有正确布局。
- [x] 在可用测试会话中，分别在输入文本、待确认转写及录音状态下缩放，确认无组件重挂载、会话重置或重复开始请求；麦克风测试需要可用设备与权限，无法验证时明确记录。

## 任务 6：全站验收与交付

**文件：** 新增 `docs/superpowers/plans/2026-09-21-client-responsive-validation.md`，只记录实际执行的结果、问题和截图位置，不预填通过。

- [x] 在前台目录执行构建：

```powershell
Set-Location 'C:\Users\HP\Desktop\ai-mock-interview-platform-main\vue-front (4)'
npm.cmd run build
```

- [x] 使用已有前台服务；未运行时执行 npm.cmd run dev。不要为布局验收重启全部后端或修改数据库。
- [x] 检查路由表的全部 18 个页面：首页、上传占位、个人中心、个人信息、设置、面试历史、JD 历史、JD 输入、JD 结果、简历上传、简历结果、简历库、面试、面试总结、账号安全、登录、注册、重置密码。实施时以路由表实际数量为准，并剔除重定向项。
- [x] 全页面至少覆盖 390×844 和 1440×900；首页、认证、上传、列表、预览、面试、报告再覆盖 320×568、768×1024、1024×768、1280×600、1920×1080。
- [ ] 在 767/768px 和 1199/1200px 两侧拖动窗口；检查 80%、100%、125%、200% 的真实浏览器缩放。不能只用改变视口冒充浏览器缩放测试。
- [ ] 验证无非预期页面级横向滚动，按钮不遮挡，键盘焦点可达；PDF/文档等确需横向滚动的内容只能在局部容器内滚动。
- [x] 记录跨断点的表单、文件、聊天和录音状态检查结果；真实移动设备软键盘未测时，标记未验证，不能以桌面模拟替代。
- [x] 对照改造前截图确认配色、卡片及人物风格保留；汇报构建结果、测试覆盖和外部服务导致的未验证项。

## 执行顺序与完成定义

按任务 1 → 2 → 3 → 4 → 5 → 6 顺序实施。每项通过自己的页面检查后再进入下一项。任务 1 完成不等于全站适配完成。

完成条件：自动布局覆盖所有现有候选人路由；跨断点和缩放时核心操作可达，业务状态保留；构建通过，验收记录如实区分通过、失败与未验证。本计划不承诺未运行的后端服务、AI 接口或硬件录音已通过测试。
