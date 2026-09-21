package com.example.springbootfront.dto.resume;

public class ResumeAnalysisRequest {

    private Long resumeId;
    private String targetJdText;

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getTargetJdText() {
        return targetJdText;
    }

    public void setTargetJdText(String targetJdText) {
        this.targetJdText = targetJdText;
    }
}
