package interview_elf.resume_cut.service;

import interview_elf.resume_cut.dto.ResumeResultDTO;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public interface ResumeParserService {

    // ================= 核心原子能力 =================
    /**
     * 解析单个文件流（基础方法）
     */
    ResumeResultDTO parse(InputStream inputStream, String fileName);

    // ================= 后台/本地 批量处理能力 =================
    /**
     * 批量解析本地文件列表（供后台逻辑调用）
     * 默认实现：循环调用基础 parse 方法，聚合结果
     */
    default List<ResumeResultDTO> parseLocalBatch(List<File> fileList) {
        List<ResumeResultDTO> results = new ArrayList<>();
        if (fileList == null || fileList.isEmpty()) return results;

        for (File file : fileList) {
            try (FileInputStream fis = new FileInputStream(file)) {
                // 复用核心解析逻辑
                ResumeResultDTO result = parse(fis, file.getName());
                results.add(result);
            } catch (Exception e) {
                // 捕获单个文件的异常，防止打断整个批次
                ResumeResultDTO errorResult = new ResumeResultDTO();
                errorResult.setErrorMessage("文件读取失败: " + file.getName());
                results.add(errorResult);
            }
        }
        return results;
    }
}