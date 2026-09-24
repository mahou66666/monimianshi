// Synthesized with Windows Huihui; speech scores are fixtures, not acoustic measurements.
export const audioSamples = [
  {
    "id": "java-cache",
    "roleId": "java",
    "text": "我采用旁路缓存模式，先更新数据库再删除缓存。删除失败时记录重试任务，通过幂等处理保证最终一致性，并监控重试积压。",
    "file": "/audio/java-cache.wav",
    "duration": 16.52,
    "speed": 203,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "java-index",
    "roleId": "java",
    "text": "先比较慢查询与历史基线，检查数据量、锁等待和执行计划，再验证索引选择、扫描行数及返回数据量，最后通过压测确认优化。",
    "file": "/audio/java-index.wav",
    "duration": 16.97,
    "speed": 202,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "java-order",
    "roleId": "java",
    "text": "客户端携带幂等键，服务端通过唯一约束防止重复写入，在事务中记录订单和结果。重复请求返回同一结果，并明确超时后的重试语义。",
    "file": "/audio/java-order.wav",
    "duration": 17.27,
    "speed": 208,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "java-team",
    "roleId": "java",
    "text": "背景是预约接口在高峰期超时。我负责慢查询定位并与同学分工验证缓存策略。行动是建立监控基线并逐步压测，结果是达到目标响应时间，复盘中补充了容量预案。",
    "file": "/audio/java-team.wav",
    "duration": 21.21,
    "speed": 207,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "web-render",
    "roleId": "web",
    "text": "先用指标定位网络和渲染瓶颈，区分资源下载与主线程阻塞，再做路由拆分、图片压缩和缓存优化，最后在相同设备与网络下验证。",
    "file": "/audio/web-render.wav",
    "duration": 16.35,
    "speed": 213,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "web-state",
    "roleId": "web",
    "text": "只影响单个组件的状态放在组件内部，跨页面共享的数据集中管理。坚持单向数据流，明确状态的生命周期，通过事件表达变化。",
    "file": "/audio/web-state.wav",
    "duration": 16.14,
    "speed": 212,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "web-race",
    "roleId": "web",
    "text": "输入先做防抖，通过取消旧请求和请求序号校验确保只应用最新结果，同时处理取消与网络异常，保留明确的加载状态。",
    "file": "/audio/web-race.wav",
    "duration": 15.03,
    "speed": 212,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "web-team",
    "roleId": "web",
    "text": "先确认用户目标和限制，提出不同成本的方案，用原型验证关键交互并收集反馈，最后记录取舍和验收标准。",
    "file": "/audio/web-team.wav",
    "duration": 13.59,
    "speed": 212,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "algo-leak",
    "roleId": "algorithm",
    "text": "先检查训练和验证划分是否有数据泄漏，再比较线上线下数据分布与特征生成逻辑，以基线和分层评估定位问题，并排除指标口径变化。",
    "file": "/audio/algo-leak.wav",
    "duration": 16.94,
    "speed": 213,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "algo-metric",
    "roleId": "algorithm",
    "text": "全预测负类也可能有很高准确率。我结合精确率和召回率、PR 曲线及业务成本选择阈值，并在独立测试集验证。",
    "file": "/audio/algo-metric.wav",
    "duration": 14.37,
    "speed": 213,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "algo-serve",
    "roleId": "algorithm",
    "text": "先明确延迟和吞吐约束，再比较量化、蒸馏等方案，用对照实验评估收益与成本，灰度上线并准备监控和回滚。",
    "file": "/audio/algo-serve.wav",
    "duration": 14.58,
    "speed": 202,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "algo-team",
    "roleId": "algorithm",
    "text": "固定数据版本并记录随机种子，重复对照实验观察波动，记录超参数与环境，对不可复现的收益保持谨慎并报告限制。",
    "file": "/audio/algo-team.wav",
    "duration": 14.34,
    "speed": 218,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "test-cases",
    "roleId": "testing",
    "text": "先明确叠加规则，用等价类和边界值覆盖金额、有效期及数量，再设计互斥条件组合，检查异常输入、并发使用和状态变化。",
    "file": "/audio/test-cases.wav",
    "duration": 16.09,
    "speed": 205,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "test-api",
    "roleId": "testing",
    "text": "状态码只是第一层。我会验证业务码、响应数据结构和字段值，并检查数据库、消息等副作用，覆盖鉴权和异常路径。",
    "file": "/audio/test-api.wav",
    "duration": 15.72,
    "speed": 198,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "test-flaky",
    "roleId": "testing",
    "text": "先保留失败日志与环境信息，检查数据隔离、异步等待和外部依赖，再建立确定性的等待条件与替身，不能仅增加重试掩盖失败。",
    "file": "/audio/test-flaky.wav",
    "duration": 16.0,
    "speed": 214,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  },
  {
    "id": "test-team",
    "roleId": "testing",
    "text": "整理可复现步骤和用户影响，结合概率与严重程度说明风险，提出修复、降级或延期方案，并明确回归范围和发布监控。",
    "file": "/audio/test-team.wav",
    "duration": 15.2,
    "speed": 209,
    "score": 76,
    "clarity": "良好（示例）",
    "confidence": "较稳定（示例估计）"
  }
];
export const findAudioSample = id => audioSamples.find(s => s.id === id);
