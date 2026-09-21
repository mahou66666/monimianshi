package interview_elf.resume_cut.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.benjaminwan.ocrlibrary.OcrResult;
import com.benjaminwan.ocrlibrary.TextBlock;
import interview_elf.resume_cut.dto.ResumeResultDTO;
import interview_elf.resume_cut.service.ResumeParserService;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

@Service
public class ResumeParserServiceImpl implements ResumeParserService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParserServiceImpl.class);
    private static final int MIN_EXTRACTED_TEXT_CHARS = 30;
    private static final int MAX_OCR_PAGES = 5;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private InferenceEngine ocrEngine;

    @Value("${llm.api.url:https://api.siliconflow.cn/v1/chat/completions}")
    private String llmApiUrl;

    @Value("${llm.api.key:}")
    private String llmApiKey;

    @Value("${llm.model:Qwen/Qwen3.8-27B}")
    private String llmModel;

    // 预编译正则：清洗无意义字符，减少 Input Token，提升速度
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[ \\t\\f]+");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");
    private static final Pattern SCHOOL_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9·()（）\\-]{2,40}(大学|学院|学校|研究院|职业技术学院))");
    private static final Pattern DEGREE_PATTERN = Pattern.compile("(博士研究生|硕士研究生|研究生|博士|硕士|本科|学士|专科|大专|中专|MBA|EMBA)");
    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("((19|20)\\d{2}[./-]?(0?[1-9]|1[0-2])?)\\s*[-~—至]\\s*((19|20)\\d{2}[./-]?(0?[1-9]|1[0-2])?|至今|现在)");

    public ResumeParserServiceImpl() {
        // 配置长超时，适应完整内容生成的耗时
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(120000); // 2分钟读取超时
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public ResumeResultDTO parse(InputStream inputStream, String fileName) {
        String lowerName = fileName.toLowerCase();
        String fullText = "";
        ResumeResultDTO result = new ResumeResultDTO();

        long startTime = System.currentTimeMillis();

        try {
            // 1. 提取
            if (lowerName.endsWith(".pdf")) {
                fullText = extractTextFromPdf(inputStream);
            } else if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
                fullText = extractTextFromWord(inputStream);
            } else {
                throw new IllegalArgumentException("不支持的文件格式: " + fileName);
            }

            // ★ 核心保障：先存原始文本
            result.setRawContent(fullText);

            // 2. 清洗 (优化 Input 速度)
            String cleanText = optimizeTextForAi(fullText);
            if (meaningfulLength(cleanText) < MIN_EXTRACTED_TEXT_CHARS) {
                result.setErrorMessage("未能从文件中提取到足够文字，可能是扫描版 PDF 或图片型简历，请上传可复制文字的 PDF/DOCX");
                log.warn("简历文本提取为空或过短，已跳过 AI 解析: fileName={}, length={}",
                        fileName, meaningfulLength(cleanText));
                return result;
            }

            // 3. AI 解析
            ResumeResultDTO aiResult = parseContentWithDeepSeek(cleanText);

            // 4. 合并
            if (aiResult != null) {
                result.setBasicInfo(aiResult.getBasicInfo());
                result.setSkills(aiResult.getSkills());
                result.setEducation(normalizeEducation(aiResult.getEducation(), fullText, aiResult.getBasicInfo()));
                result.setWorkExperience(aiResult.getWorkExperience());
                result.setProjectExperience(aiResult.getProjectExperience());
                result.setInternshipExperience(aiResult.getInternshipExperience());
                result.setAwards(aiResult.getAwards());
                result.setSelfEvaluation(aiResult.getSelfEvaluation());
            } else {
                result.setEducation(normalizeEducation(null, fullText, null));
            }

            log.info("简历解析完成，耗时: {}ms", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            log.error("文件读取失败", e);
            result.setErrorMessage("文件读取失败: " + e.getMessage());
            // 确保异常时也有文本
            if (result.getRawContent() == null) result.setRawContent(fullText);
        } catch (Exception e) {
            log.error("解析异常", e);
            result.setErrorMessage("解析异常: " + e.getMessage());
            if (result.getRawContent() == null) result.setRawContent(fullText);
        }
        return result;
    }

    private ResumeResultDTO parseContentWithDeepSeek(String resumeContent) {
        ResumeResultDTO resultDTO = new ResumeResultDTO();

        if (llmApiKey == null || llmApiKey.isEmpty()) {
            resultDTO.setErrorMessage("Missing API Key");
            return resultDTO;
        }
        if (meaningfulLength(resumeContent) < MIN_EXTRACTED_TEXT_CHARS) {
            resultDTO.setErrorMessage("未能从文件中提取到足够文字，已跳过 AI 解析");
            return resultDTO;
        }

        // Prompt：保留完整性 + 压缩输出 JSON
        String prompt = "你是一个简历解析助手。请将以下简历解析为 JSON。\n" +
                "规则：\n" +
                "1. **完整提取**：必须原封不动地保留简历中所有的描述细节（包括项目背景、技术栈、职责等），严禁摘要或删减。\n" +
                "2. **压缩输出**：返回 **单行 Minified JSON**，不要使用换行符或缩进，以最快速度输出。\n" +
                "3. 字段结构：\n" +
                "   - BASIC_INFO: {name, phone, email, age, university, degree, job_intention(可以是字符串或对象)}\n" +
                "   - SKILLS: 完整内容 (可以是字符串或列表)。\n" +
                "   - EDUCATION: 字符串列表。每一项必须是“学校+学历(+时间)”的教育经历，如“浙江师范大学 本科 2023.09-至今”；课程成绩不要放到 EDUCATION。\n" +
                "   - WORK_EXPERIENCE, PROJECT_EXPERIENCE: 字符串列表。保留原始段落换行。\n" +
                "   - AWARDS, SELF_EVALUATION: 完整内容 (可以是字符串或列表)。\n" +
                "4. 字段不存在则忽略（不返回 key）。\n" +
                "\n" +
                "简历内容：\n" +
                resumeContent;

        if (prompt.length() > 28000) {
            prompt = prompt.substring(0, 28000);
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmModel);
            requestBody.put("enable_thinking", false); // 简历字段提取不启用思考模式
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.0); // 0.0 确保最稳最快
            requestBody.put("response_format", Map.of("type", "json_object"));

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            requestBody.put("messages", Collections.singletonList(userMessage));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(llmApiUrl, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    return objectMapper.readValue(content, ResumeResultDTO.class);
                }
            }
        } catch (Exception e) {
            log.error("AI API 调用失败", e);
            throw new RuntimeException("AI调用异常: " + e.getMessage());
        }
        return resultDTO;
    }

    private String optimizeTextForAi(String raw) {
        if (raw == null) return "";
        String text = MULTIPLE_NEWLINES.matcher(raw).replaceAll("\n");
        text = MULTIPLE_SPACES.matcher(text).replaceAll(" ");
        return text.trim();
    }

    private int meaningfulLength(String value) {
        if (value == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private List<String> normalizeEducation(List<String> aiEducation, String rawText, ResumeResultDTO.BasicInfo basicInfo) {
        LinkedHashSet<String> entries = new LinkedHashSet<>();

        if (basicInfo != null) {
            String fromBasicInfo = buildEducationEntryFromParts(basicInfo.getUniversity(), basicInfo.getDegree(), null);
            if (fromBasicInfo != null) {
                entries.add(fromBasicInfo);
            }
        }

        if (aiEducation != null) {
            for (String block : aiEducation) {
                if (block == null || block.isBlank()) {
                    continue;
                }
                for (String line : block.split("\\n")) {
                    String normalized = normalizeEducationLine(line);
                    if (normalized != null) {
                        entries.add(normalized);
                    }
                }
            }
        }

        for (String extracted : extractEducationFromRawText(rawText)) {
            entries.add(extracted);
        }

        return collapseEducationEntries(entries);
    }

    private List<String> extractEducationFromRawText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        boolean inEducationSection = false;

        for (String rawLine : rawText.split("\\n")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            if (line.contains("教育背景") || line.contains("教育经历") || line.toLowerCase().contains("education")) {
                inEducationSection = true;
            } else if (line.contains("工作经历") || line.contains("项目经历") || line.contains("实习经历")
                    || line.contains("校园经历") || line.contains("职业技能") || line.contains("奖项荣誉")
                    || line.contains("自我评价") || line.contains("联系方式")) {
                inEducationSection = false;
            }

            if (inEducationSection || looksLikeEducationLine(line)) {
                String normalized = normalizeEducationLine(line);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
        }

        return new ArrayList<>(result);
    }

    private String normalizeEducationLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String clean = MULTIPLE_SPACES.matcher(line.trim()).replaceAll(" ");
        if (!looksLikeEducationLine(clean)) {
            return null;
        }

        Matcher schoolMatcher = SCHOOL_PATTERN.matcher(clean);
        Matcher degreeMatcher = DEGREE_PATTERN.matcher(clean);
        Matcher yearMatcher = YEAR_RANGE_PATTERN.matcher(clean);

        String school = schoolMatcher.find() ? schoolMatcher.group(1).trim() : null;
        String degree = degreeMatcher.find() ? degreeMatcher.group(1).trim() : null;
        String yearRange = yearMatcher.find() ? yearMatcher.group().trim() : null;

        return buildEducationEntryFromParts(school, degree, yearRange);
    }

    private boolean looksLikeEducationLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        boolean hasSchool = SCHOOL_PATTERN.matcher(line).find();
        boolean hasDegree = DEGREE_PATTERN.matcher(line).find();
        boolean hasTime = YEAR_RANGE_PATTERN.matcher(line).find();
        return (hasSchool && hasDegree) || (hasSchool && hasTime);
    }

    private String buildEducationEntryFromParts(String school, String degree, String yearRange) {
        if (school == null || school.isBlank() || degree == null || degree.isBlank()) {
            return null;
        }
        if (yearRange != null && !yearRange.isBlank()) {
            return (school + " " + degree + " " + yearRange).trim();
        }
        return (school + " " + degree).trim();
    }


    private List<String> collapseEducationEntries(LinkedHashSet<String> rawEntries) {
        if (rawEntries.isEmpty()) {
            return null;
        }

        LinkedHashMap<String, String> byKey = new LinkedHashMap<>();
        for (String entry : rawEntries) {
            Matcher schoolMatcher = SCHOOL_PATTERN.matcher(entry);
            Matcher degreeMatcher = DEGREE_PATTERN.matcher(entry);
            if (!schoolMatcher.find() || !degreeMatcher.find()) {
                continue;
            }

            String key = schoolMatcher.group(1).trim() + "#" + degreeMatcher.group(1).trim();
            String existing = byKey.get(key);
            boolean currentHasTime = YEAR_RANGE_PATTERN.matcher(entry).find();
            boolean existingHasTime = existing != null && YEAR_RANGE_PATTERN.matcher(existing).find();

            if (existing == null || (currentHasTime && !existingHasTime)) {
                byKey.put(key, entry);
            }
        }

        return byKey.isEmpty() ? null : new ArrayList<>(byKey.values());
    }

    // PDF/Word 提取方法保持不变...
    private String extractTextFromWord(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(is)) {
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    sb.append(((XWPFParagraph) element).getText()).append("\n");
                } else if (element instanceof XWPFTable) {
                    for (XWPFTableRow row : ((XWPFTable) element).getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            sb.append(cell.getText()).append(" ");
                        }
                        sb.append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private String extractTextFromPdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (meaningfulLength(text) >= MIN_EXTRACTED_TEXT_CHARS) {
                return text;
            }
            log.info("PDF 文本层为空或过短，尝试 OCR 识别");
            return extractTextFromPdfByOcr(document);
        }
    }

    private String extractTextFromPdfByOcr(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder text = new StringBuilder();
        int pages = Math.min(document.getNumberOfPages(), MAX_OCR_PAGES);
        Path tempDir = Files.createTempDirectory("resume-ocr-");
        try {
            for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 200, ImageType.RGB);
                Path imagePath = tempDir.resolve("page-" + (pageIndex + 1) + ".png");
                ImageIO.write(image, "png", imagePath.toFile());
                String pageText = runOcr(imagePath);
                if (pageText != null && !pageText.isBlank()) {
                    text.append(pageText.trim()).append("\n");
                }
            }
        } finally {
            deleteQuietly(tempDir);
        }
        return text.toString();
    }

    private String runOcr(Path imagePath) {
        try {
            OcrResult result = getOcrEngine().runOcr(imagePath.toString());
            if (result == null) {
                return "";
            }
            if (result.getTextBlocks() != null && !result.getTextBlocks().isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (TextBlock block : result.getTextBlocks()) {
                    if (block.getText() != null && !block.getText().isBlank()) {
                        builder.append(block.getText().trim()).append("\n");
                    }
                }
                return builder.toString();
            }
            return result.getStrRes();
        } catch (Exception ex) {
            log.warn("OCR 识别失败: {}", imagePath, ex);
            return "";
        }
    }

    private InferenceEngine getOcrEngine() {
        if (ocrEngine == null) {
            ocrEngine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
        }
        return ocrEngine;
    }

    private void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                try (var children = Files.list(path)) {
                    children.forEach(this::deleteQuietly);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
