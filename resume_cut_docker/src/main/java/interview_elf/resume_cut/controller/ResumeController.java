package interview_elf.resume_cut.controller;

import interview_elf.resume_cut.dto.ResumeResultDTO;
import interview_elf.resume_cut.service.ResumeParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeParserService resumeParserService;

    /**
     * 1. 单文件解析
     */
    @PostMapping("/parse")
    public ResponseEntity<ResumeResultDTO> parseOne(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(resumeParserService.parse(file.getInputStream(), file.getOriginalFilename()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build(); // 简化处理
        }
    }

    /**
     * 2. 批量解析接口 (前台调用)
     * 接收文件列表 -> 全部处理完成 -> 返回结果列表
     */
    @PostMapping("/parse/batch")
    public ResponseEntity<List<ResumeResultDTO>> parseBatch(@RequestParam("files") MultipartFile[] files) {
        List<ResumeResultDTO> results = new ArrayList<>();

        // 遍历处理
        for (MultipartFile file : files) {
            try {
                if (!file.isEmpty()) {
                    // 调用核心服务
                    ResumeResultDTO result = resumeParserService.parse(file.getInputStream(), file.getOriginalFilename());
                    results.add(result);
                }
            } catch (Exception e) {
                // 记录错误但不中断整个批次
                ResumeResultDTO errorDTO = new ResumeResultDTO();
                errorDTO.setRawContent("File: " + file.getOriginalFilename());
                errorDTO.setErrorMessage("解析失败: " + e.getMessage());
                results.add(errorDTO);
            }
        }

        // 全部处理完成后返回
        return ResponseEntity.ok(results);
    }
}