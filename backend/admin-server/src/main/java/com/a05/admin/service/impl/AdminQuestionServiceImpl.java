package com.a05.admin.service.impl;

import com.a05.admin.controller.dto.GenerateGeneralQuestionsRequest;
import com.a05.admin.controller.dto.GeneralQuestionQuery;
import com.a05.admin.controller.dto.UpdateGeneralQuestionRequest;
import com.a05.admin.entity.ResumeProblem;
import com.a05.admin.mapper.ResumeProblemMapper;
import com.a05.admin.service.AdminQuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AdminQuestionServiceImpl implements AdminQuestionService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "internship", "project", "education", "certificate", "jd");

    private static final List<String> MODEL_KEYS = Arrays.asList("ds", "sd", "doubao", "kimi");

    private final ResumeProblemMapper resumeProblemMapper;
    private final ObjectMapper objectMapper;

    public AdminQuestionServiceImpl(ResumeProblemMapper resumeProblemMapper, ObjectMapper objectMapper) {
        this.resumeProblemMapper = resumeProblemMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> listQuestions(String category, GeneralQuestionQuery query) {
        String normalizedCategory = normalizeCategory(category);
        GeneralQuestionQuery safeQuery = query == null ? new GeneralQuestionQuery() : query;

        int pageNo = normalizePageNo(safeQuery.getPageNo());
        int pageSize = normalizePageSize(safeQuery.getPageSize());

        LambdaQueryWrapper<ResumeProblem> wrapper = new LambdaQueryWrapper<ResumeProblem>()
                .eq(ResumeProblem::getPriority, normalizedCategory)
                .orderByDesc(ResumeProblem::getId);

        if (safeQuery.getStatus() != null) {
            wrapper.eq(ResumeProblem::getStatus, safeQuery.getStatus());
        }
        if (StringUtils.hasText(safeQuery.getKeyword())) {
            String keyword = safeQuery.getKeyword().trim();
            wrapper.and(w -> w.like(ResumeProblem::getProblemTitle, keyword)
                    .or().like(ResumeProblem::getProblemDesc, keyword)
                    .or().like(ResumeProblem::getSuggestion, keyword));
        }

        List<ResumeProblem> all = resumeProblemMapper.selectList(wrapper);
        int total = all.size();
        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        List<ResumeProblem> page = fromIndex >= total ? new ArrayList<>() : all.subList(fromIndex, toIndex);

        List<Map<String, Object>> items = new ArrayList<>();
        for (ResumeProblem problem : page) {
            items.add(toQuestionItem(problem, normalizedCategory));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pageNo", pageNo);
        data.put("pageSize", pageSize);
        data.put("total", total);
        data.put("items", items);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateQuestions(String category, GenerateGeneralQuestionsRequest request) {
        String normalizedCategory = normalizeCategory(category);
        GenerateGeneralQuestionsRequest safeRequest = request == null ? new GenerateGeneralQuestionsRequest() : request;
        int count = normalizeGenerateCount(safeRequest.getCount());
        String model = normalizeModel(safeRequest.getModel());

        Long resumeId = resolveResumeId(safeRequest.getResumeId(), safeRequest.getRelatedId());
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String question = buildQuestion(normalizedCategory, i, safeRequest);
            String answer = buildAnswer(normalizedCategory, i);

            ResumeProblem row = new ResumeProblem();
            row.setResumeId(resumeId);
            row.setProblemTitle(trimToMax(question, 100));
            row.setProblemDesc(trimToMax(answer, 500));
            row.setPriority(normalizedCategory);
            row.setStatus(1);

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("questionAttr", defaultQuestionAttr(normalizedCategory));
            meta.put("questionType", 1);
            meta.put("difficulty", 2);
            meta.put("isCommon", 1);
            meta.put("bankId", normalizedCategory + "-bank");
            meta.put("bankName", categoryDisplay(normalizedCategory) + "题库");
            meta.put("keywords", buildKeywords(normalizedCategory, question, answer));
            meta.put("model", model);
            meta.put("source", "rule");
            meta.put("userId", safeText(safeRequest.getUserId()));
            meta.put("relatedId", safeText(safeRequest.getRelatedId()));
            meta.put("resumeId", safeText(safeRequest.getResumeId()));

            for (String modelKey : MODEL_KEYS) {
                if (modelKey.equals(model)) {
                    meta.put(modelKey, buildModelDetail(question, answer, modelKey));
                } else {
                    meta.put(modelKey, emptyModelDetail());
                }
            }

            row.setSuggestion(toJson(meta));
            resumeProblemMapper.insert(row);

            items.add(toQuestionItem(row, normalizedCategory));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("category", normalizedCategory);
        data.put("createdCount", items.size());
        data.put("items", items);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateQuestion(String category, Long id, UpdateGeneralQuestionRequest request) {
        String normalizedCategory = normalizeCategory(category);
        ResumeProblem row = requireCategoryQuestion(id, normalizedCategory);
        UpdateGeneralQuestionRequest safeRequest = request == null ? new UpdateGeneralQuestionRequest() : request;

        if (safeRequest.getQuestionContent() != null) {
            String questionContent = safeText(safeRequest.getQuestionContent());
            if (!StringUtils.hasText(questionContent)) {
                throw new RuntimeException("questionContent cannot be empty");
            }
            row.setProblemTitle(trimToMax(questionContent, 100));
        }
        if (safeRequest.getAnswerContent() != null) {
            row.setProblemDesc(trimToMax(safeText(safeRequest.getAnswerContent()), 500));
        }

        if (safeRequest.getStatus() != null) {
            row.setStatus(normalizeStatus(safeRequest.getStatus()));
        }

        Map<String, Object> meta = parseMeta(row.getSuggestion());
        if (safeRequest.getQuestionAttr() != null) {
            meta.put("questionAttr", trimToMax(safeText(safeRequest.getQuestionAttr()), 100));
        }
        if (safeRequest.getKeywords() != null) {
            meta.put("keywords", trimToMax(safeText(safeRequest.getKeywords()), 255));
        }
        for (String modelKey : MODEL_KEYS) {
            if (!meta.containsKey(modelKey)) {
                meta.put(modelKey, emptyModelDetail());
            } else {
                meta.put(modelKey, normalizeModelDetail(meta.get(modelKey)));
            }
        }
        row.setSuggestion(toJson(meta));

        resumeProblemMapper.updateById(row);
        ResumeProblem updated = resumeProblemMapper.selectById(row.getId());
        return toQuestionItem(updated == null ? row : updated, normalizedCategory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteQuestion(String category, Long id) {
        String normalizedCategory = normalizeCategory(category);
        requireCategoryQuestion(id, normalizedCategory);
        resumeProblemMapper.deleteById(id);

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("id", id);
        return data;
    }

    private ResumeProblem requireCategoryQuestion(Long id, String category) {
        if (id == null || id <= 0) {
            throw new RuntimeException("id cannot be empty");
        }
        ResumeProblem row = resumeProblemMapper.selectById(id);
        if (row == null) {
            throw new RuntimeException("question not found");
        }
        if (!category.equalsIgnoreCase(safeText(row.getPriority()))) {
            throw new RuntimeException("question category mismatch");
        }
        return row;
    }

    private Map<String, Object> toQuestionItem(ResumeProblem row, String category) {
        Map<String, Object> meta = parseMeta(row.getSuggestion());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.getId());
        item.put("questionContent", safeText(row.getProblemTitle()));
        item.put("answerContent", safeText(row.getProblemDesc()));
        item.put("status", row.getStatus() == null ? 1 : row.getStatus());
        item.put("questionAttr", getMetaText(meta, "questionAttr", defaultQuestionAttr(category)));
        item.put("questionType", getMetaInt(meta, "questionType", 1));
        item.put("difficulty", getMetaInt(meta, "difficulty", 2));
        item.put("isCommon", getMetaInt(meta, "isCommon", 1));
        item.put("bankId", getMetaValue(meta, "bankId", category + "-bank"));
        item.put("bankName", getMetaText(meta, "bankName", categoryDisplay(category) + "题库"));
        item.put("keywords", getMetaText(meta, "keywords", ""));
        item.put("ds", normalizeModelDetail(meta.get("ds")));
        item.put("sd", normalizeModelDetail(meta.get("sd")));
        item.put("doubao", normalizeModelDetail(meta.get("doubao")));
        item.put("kimi", normalizeModelDetail(meta.get("kimi")));
        return item;
    }

    private Map<String, Object> parseMeta(String suggestion) {
        if (!StringUtils.hasText(suggestion)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> data = objectMapper.readValue(
                    suggestion, new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            return data == null ? new LinkedHashMap<>() : data;
        } catch (Exception ex) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("legacySuggestion", suggestion);
            return fallback;
        }
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Map.of() : data);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, Object> normalizeModelDetail(Object raw) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (raw instanceof Map<?, ?> map) {
            items.addAll(normalizeQaItems(map.get("items")));
        } else {
            items.addAll(normalizeQaItems(raw));
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("items", items);
        return detail;
    }

    private List<Map<String, Object>> normalizeQaItems(Object raw) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> qa = normalizeQaItem(item);
                if (qa != null) {
                    items.add(qa);
                }
            }
            return items;
        }
        Map<String, Object> one = normalizeQaItem(raw);
        if (one != null) {
            items.add(one);
        }
        return items;
    }

    private Map<String, Object> normalizeQaItem(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Map<?, ?> map) {
            String q = safeText(valueToString(map.get("question")));
            String a = safeText(valueToString(map.get("answer")));
            if (!StringUtils.hasText(q) && !StringUtils.hasText(a)) {
                return null;
            }
            Map<String, Object> qa = new LinkedHashMap<>();
            qa.put("question", q);
            qa.put("answer", a);
            return qa;
        }
        String text = safeText(valueToString(raw));
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Map<String, Object> qa = new LinkedHashMap<>();
        qa.put("question", text);
        qa.put("answer", "");
        return qa;
    }

    private Map<String, Object> buildModelDetail(String question, String answer, String model) {
        Map<String, Object> detail = new LinkedHashMap<>();
        Map<String, Object> qa = new LinkedHashMap<>();
        qa.put("question", question);
        qa.put("answer", answer);
        detail.put("items", List.of(qa));
        detail.put("model", model);
        return detail;
    }

    private Map<String, Object> emptyModelDetail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("items", new ArrayList<>());
        return detail;
    }

    private Object getMetaValue(Map<String, Object> meta, String key, Object defaultValue) {
        if (meta == null || !meta.containsKey(key)) {
            return defaultValue;
        }
        Object value = meta.get(key);
        return value == null ? defaultValue : value;
    }

    private String getMetaText(Map<String, Object> meta, String key, String defaultValue) {
        Object value = getMetaValue(meta, key, defaultValue);
        String text = safeText(valueToString(value));
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private Integer getMetaInt(Map<String, Object> meta, String key, Integer defaultValue) {
        Object value = getMetaValue(meta, key, defaultValue);
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            String text = safeText(valueToString(value));
            if (!StringUtils.hasText(text)) {
                return defaultValue;
            }
            return Integer.parseInt(text);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String normalizeCategory(String category) {
        String value = safeText(category).toLowerCase(Locale.ROOT);
        if (!ALLOWED_CATEGORIES.contains(value)) {
            throw new RuntimeException("unsupported category: " + category);
        }
        return value;
    }

    private String categoryDisplay(String category) {
        return switch (category) {
            case "internship" -> "实习";
            case "project" -> "项目";
            case "education" -> "教育";
            case "certificate" -> "证书";
            case "jd" -> "JD";
            default -> "通用";
        };
    }

    private String defaultQuestionAttr(String category) {
        return categoryDisplay(category) + "场景";
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }

    private int normalizeGenerateCount(Integer count) {
        if (count == null || count < 1) {
            return 10;
        }
        return Math.min(count, 50);
    }

    private int normalizeStatus(Integer status) {
        return status != null && status == 2 ? 2 : 1;
    }

    private String normalizeModel(String model) {
        String value = safeText(model).toLowerCase(Locale.ROOT);
        if (!MODEL_KEYS.contains(value)) {
            return "ds";
        }
        return value;
    }

    private Long resolveResumeId(String resumeId, String relatedId) {
        Long fromResume = parsePositiveLong(resumeId);
        if (fromResume != null) {
            return fromResume;
        }
        Long fromRelated = parsePositiveLong(relatedId);
        if (fromRelated != null) {
            return fromRelated;
        }
        return 0L;
    }

    private Long parsePositiveLong(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            long value = Long.parseLong(text.trim());
            return value > 0 ? value : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildQuestion(String category, int index, GenerateGeneralQuestionsRequest request) {
        String target = safeText(request.getRelatedId());
        if (!StringUtils.hasText(target)) {
            target = safeText(request.getResumeId());
        }
        String suffix = index > 1 ? "（变体" + index + "）" : "";
        return switch (category) {
            case "internship" -> "请描述一次你在实习中快速接手任务并按期交付的经历" + suffix;
            case "project" -> "你做过的项目里，最能体现技术深度的是哪一个？请展开说明" + suffix;
            case "education" -> "你的教育经历里，哪些课程或训练对岗位最有帮助？" + suffix;
            case "certificate" -> "你拿到过哪些证书或奖项？它们如何帮助你胜任岗位？" + suffix;
            case "jd" -> "针对JD " + (StringUtils.hasText(target) ? target : "目标岗位") + "，你会如何拆解能力要求并准备面试？" + suffix;
            default -> "请结合个人经历回答一个高频面试问题" + suffix;
        };
    }

    private String buildAnswer(String category, int index) {
        return switch (category) {
            case "internship" -> "建议按 STAR 结构回答：场景、任务、行动、结果，并补充可量化指标。";
            case "project" -> "重点说明你的角色、关键技术决策、难点突破和结果指标，体现个人贡献。";
            case "education" -> "突出与岗位匹配的课程和实践，说明知识如何落地到项目或实习成果。";
            case "certificate" -> "说明证书/奖项含金量、获得过程与岗位能力映射，避免只列名称。";
            case "jd" -> "先提炼JD关键词，再匹配经历证据，最后补充短板改进计划与时间安排。";
            default -> "给出结构化回答，先结论后细节，并补充真实数据增强说服力。";
        } + (index > 1 ? "（第" + index + "题）" : "");
    }

    private String buildKeywords(String category, String question, String answer) {
        List<String> parts = new ArrayList<>();
        parts.add(categoryDisplay(category));
        if (StringUtils.hasText(question) && question.length() >= 4) {
            parts.add(question.substring(0, Math.min(8, question.length())));
        }
        if (StringUtils.hasText(answer) && answer.length() >= 4) {
            parts.add(answer.substring(0, Math.min(8, answer.length())));
        }
        return String.join(",", parts);
    }

    private String trimToMax(String text, int maxLen) {
        String value = safeText(text);
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String valueToString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
