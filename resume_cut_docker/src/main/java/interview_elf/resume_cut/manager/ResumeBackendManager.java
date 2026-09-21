package interview_elf.resume_cut.manager;

import interview_elf.resume_cut.dto.ResumeResultDTO;
import interview_elf.resume_cut.service.ResumeParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 后台简历处理管理器
 * 统一封装后台对本地文件的操作（单体、目录、列表）
 */
@Component
public class ResumeBackendManager {

    private static final Logger log = LoggerFactory.getLogger(ResumeBackendManager.class);

    @Autowired
    private ResumeParserService resumeParserService;

    // 支持的文件扩展名
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(".pdf", ".doc", ".docx");

    /**
     * 功能 1: 处理单体文件
     */
    public ResumeResultDTO processSingleFile(String filePath) {
        File file = new File(filePath);
        if (!checkFileValid(file)) {
            ResumeResultDTO error = new ResumeResultDTO();
            error.setErrorMessage("文件无效或不支持: " + filePath);
            return error;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return resumeParserService.parse(fis, file.getName());
        } catch (Exception e) {
            log.error("单体解析异常", e);
            ResumeResultDTO error = new ResumeResultDTO();
            error.setErrorMessage("系统异常: " + e.getMessage());
            return error;
        }
    }

    /**
     * 功能 2: 处理文件地址列表 (核心新增功能)
     * @param filePaths 文件绝对路径的列表
     * @return 解析结果列表
     */
    public List<ResumeResultDTO> processFilePaths(List<String> filePaths) {
        List<File> validFiles = new ArrayList<>();

        if (filePaths == null || filePaths.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 预处理：将路径转为 File 对象并过滤
        for (String path : filePaths) {
            File file = new File(path);
            if (checkFileValid(file)) {
                validFiles.add(file);
            } else {
                // 可以在这里记录日志，提示哪个文件被跳过了
                log.warn("跳过无效文件路径: {}", path);

                // 也可以选择在这里生成一个包含错误信息的 ResultDTO 放入结果集，
                // 看业务需求是"直接跳过"还是"返回错误提示"。
                // 这里演示直接跳过，交由 Service 统一处理有效文件。
            }
        }

        // 2. 调用 Service 的批量接口
        log.info("接收到 {} 个路径，有效文件 {} 个，开始处理...", filePaths.size(), validFiles.size());
        return resumeParserService.parseLocalBatch(validFiles);
    }

    /**
     * 功能 3: 扫描并处理整个目录
     */
    public List<ResumeResultDTO> processDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("目录不存在: {}", directoryPath);
            return new ArrayList<>();
        }

        File[] files = dir.listFiles((d, name) -> isSupported(name));
        if (files == null) return new ArrayList<>();

        return resumeParserService.parseLocalBatch(Arrays.asList(files));
    }

    // ================= 辅助方法 =================

    /**
     * 校验文件是否有效（存在、是文件、格式支持）
     */
    private boolean checkFileValid(File file) {
        return file.exists() && file.isFile() && isSupported(file.getName());
    }

    private boolean isSupported(String fileName) {
        String lower = fileName.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}