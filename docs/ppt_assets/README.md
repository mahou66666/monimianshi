# PPT 矢量素材说明

这些图是给后台系统 PPT 用的结构化素材，建议直接插入 PPT。SVG 是矢量格式，放大不会糊。

## 素材清单

| 文件名 | 用途 |
|---|---|
| `01_admin_system_architecture.svg` | 后台系统总体架构图 |
| `02_resume_parse_flow.svg` | 简历上传与批量提取流程图 |
| `03_rbac_permission_model.svg` | 后台权限控制模型图 |
| `04_admin_feature_map.svg` | 后台功能模块关系图 |
| `05_resume_cut_algorithm_pipeline.svg` | Resume Cut 算法内部流水线图 |
| `06_resume_cut_backend_integration.svg` | 后台接入 Resume Cut 算法链路图 |
| `07_resume_cut_output_schema.svg` | ResumeResultDTO 输出字段结构图 |
| `08_resume_cut_error_retry.svg` | 算法异常兜底与重试机制图 |

## 使用方式

1. 打开 PPT。
2. 选择“插入 -> 图片 -> 此设备”。
3. 选择 `docs/ppt_assets/` 下的 SVG 文件。
4. 如果 PPT 不支持 SVG，可以右键用浏览器打开 SVG，再截图成 PNG。

## 建议放置页

- 总体架构图：放在技术架构页。
- 简历流程图：放在简历管理与算法解析页。
- 权限模型图：放在用户与权限管理页。
- 功能模块图：放在后台系统定位或模块总览页。
- Resume Cut 算法流水线图：放在算法原理页。
- 后台接入算法链路图：放在后台与算法联调页。
- 输出字段结构图：放在结构化简历结果页。
- 异常兜底与重试图：放在系统稳定性或工程实现页。
