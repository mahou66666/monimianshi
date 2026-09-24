package com.example.springbootfront.studio;

import java.util.*;

/** Lossless raw-text ranges. Never treat extracted content as model instructions. */
public final class ResumeChunks {
    private ResumeChunks() {}
    public static List<Map<String,Object>> split(String raw) {
        List<Map<String,Object>> result=new ArrayList<>();
        String section="其他"; int start=0;
        TreeMap<Integer,String> headings=new TreeMap<>();
        java.util.regex.Matcher lines=java.util.regex.Pattern.compile("(?m)^.*$").matcher(raw);
        while(lines.find()) {
            String title=lines.group().trim();if(title.length()>25)continue;
            if(title.matches(".*(项目经历|项目经验|项目实践).*"))headings.put(lines.start(),"项目");
            else if(title.matches(".*(实习经历|实习经验).*"))headings.put(lines.start(),"实习");
            else if(title.matches(".*(工作经历|工作经验).*"))headings.put(lines.start(),"工作");
            else if(title.matches(".*(教育背景|教育经历).*"))headings.put(lines.start(),"教育");
            else if(title.matches(".*(专业技能|技术栈|技能清单).*"))headings.put(lines.start(),"技能");
        }
        // Paragraph-aligned ranges, with a hard cap; all characters remain in rawText.
        while(start<raw.length()) {
            if(headings.containsKey(start))section=headings.get(start);
            int end=Math.min(start+1200,raw.length());
            Integer next=headings.higherKey(start);if(next!=null)end=Math.min(end,next);
            int paragraph=raw.indexOf("\n\n",start);if(paragraph>=start && paragraph+2<end)end=paragraph+2;
            if(end<raw.length()) { int newline=raw.lastIndexOf('\n',end); if(newline>start+400) end=newline+1; }
            // Keep surrogate pairs intact.
            if(end<raw.length() && Character.isHighSurrogate(raw.charAt(end-1))) end--;
            String text=raw.substring(start,end);
            Map<String,Object> chunk=new LinkedHashMap<>();
            chunk.put("id",UUID.randomUUID().toString());chunk.put("section",section);chunk.put("text",text);
            chunk.put("start",start);chunk.put("end",end);result.add(chunk);start=end;
        }
        return result;
    }
}
