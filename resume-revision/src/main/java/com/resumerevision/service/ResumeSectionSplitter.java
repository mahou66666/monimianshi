package com.resumerevision.service;

import com.resumerevision.model.ResumeSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ResumeSectionSplitter {

    private static final Pattern MARKDOWN_TITLE = Pattern.compile("^#{1,6}\\s+(.+)$");
    private static final Pattern COLON_TITLE = Pattern.compile("^([\\p{IsHan}A-Za-z0-9\\s/&+()\\-]{2,30})[:：]\\s*$");
    private static final Pattern NUMBERED_TITLE = Pattern.compile("^(?:\\d+|[一二三四五六七八九十]+)[\\.、]\\s*([\\p{IsHan}A-Za-z0-9\\s/&+()\\-]{2,30})$");
    private static final Pattern BRACKET_TITLE = Pattern.compile("^[【\\[]\\s*([^\\]】]{2,30})\\s*[】\\]]\\s*$");

    private static final Map<String, String> TITLE_ALIASES = new LinkedHashMap<>();

    static {
        TITLE_ALIASES.put("个人信息", "基本信息");
        TITLE_ALIASES.put("基本信息", "基本信息");
        TITLE_ALIASES.put("联系方式", "基本信息");
        TITLE_ALIASES.put("求职意向", "求职意向");
        TITLE_ALIASES.put("教育背景", "教育经历");
        TITLE_ALIASES.put("教育经历", "教育经历");
        TITLE_ALIASES.put("工作经历", "工作经历");
        TITLE_ALIASES.put("工作经验", "工作经历");
        TITLE_ALIASES.put("实习经历", "实习经历");
        TITLE_ALIASES.put("项目经历", "项目经历");
        TITLE_ALIASES.put("项目经验", "项目经历");
        TITLE_ALIASES.put("技能", "专业技能");
        TITLE_ALIASES.put("专业技能", "专业技能");
        TITLE_ALIASES.put("技术栈", "专业技能");
        TITLE_ALIASES.put("证书", "证书与荣誉");
        TITLE_ALIASES.put("获奖情况", "证书与荣誉");
        TITLE_ALIASES.put("荣誉奖项", "证书与荣誉");
        TITLE_ALIASES.put("自我评价", "自我评价");
    }

    public List<ResumeSection> split(String resumeText) {
        String safeText = resumeText == null ? "" : resumeText.trim();
        if (safeText.isEmpty()) {
            return List.of(new ResumeSection("原始内容", ""));
        }

        String[] lines = safeText.split("\\r?\\n");
        List<ResumeSection> sections = new ArrayList<>();

        String currentTitle = "基本信息";
        StringBuilder currentContent = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            String parsedTitle = parseTitle(line);
            if (parsedTitle != null) {
                appendSection(sections, currentTitle, currentContent.toString());
                currentContent = new StringBuilder();
                currentTitle = canonicalizeTitle(parsedTitle);
            } else {
                currentContent.append(rawLine).append("\n");
            }
        }

        appendSection(sections, currentTitle, currentContent.toString());

        if (sections.isEmpty()) {
            sections.add(new ResumeSection("原始内容", safeText));
        }

        return sections;
    }

    private static String parseTitle(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        var markdownMatcher = MARKDOWN_TITLE.matcher(line);
        if (markdownMatcher.matches()) {
            return markdownMatcher.group(1).trim();
        }

        var colonMatcher = COLON_TITLE.matcher(line);
        if (colonMatcher.matches()) {
            return colonMatcher.group(1).trim();
        }

        var numberedMatcher = NUMBERED_TITLE.matcher(line);
        if (numberedMatcher.matches()) {
            return numberedMatcher.group(1).trim();
        }

        var bracketMatcher = BRACKET_TITLE.matcher(line);
        if (bracketMatcher.matches()) {
            return bracketMatcher.group(1).trim();
        }

        if (isKeywordTitle(line)) {
            return line;
        }

        return null;
    }

    private static boolean isKeywordTitle(String line) {
        if (line.length() > 20) {
            return false;
        }
        if (line.contains("。") || line.contains("，") || line.contains(";") || line.contains("；")) {
            return false;
        }
        String normalized = normalizeTitle(line);
        return TITLE_ALIASES.keySet().stream().anyMatch(normalized::contains);
    }

    private static String canonicalizeTitle(String title) {
        String normalized = normalizeTitle(title);
        for (Map.Entry<String, String> entry : TITLE_ALIASES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return normalized;
    }

    private static String normalizeTitle(String line) {
        return line
                .replace("#", "")
                .replaceAll("^(?:\\d+|[一二三四五六七八九十]+)[\\.、]\\s*", "")
                .replaceAll("^[【\\[]\\s*", "")
                .replaceAll("\\s*[】\\]]$", "")
                .replaceAll("[:：]$", "")
                .trim();
    }

    private static void appendSection(List<ResumeSection> sections, String title, String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            return;
        }

        if (!sections.isEmpty()) {
            ResumeSection tail = sections.get(sections.size() - 1);
            if (tail.sectionName().equals(title)) {
                String merged = tail.content() + "\n" + normalizedContent;
                sections.set(sections.size() - 1, new ResumeSection(title, merged.trim()));
                return;
            }
        }

        sections.add(new ResumeSection(title, normalizedContent));
    }
}
