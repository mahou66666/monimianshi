package com.a05.admin.controller.dto;

public class UpdateGeneralQuestionRequest {

    private String questionContent;
    private String answerContent;
    private String questionAttr;
    private String keywords;
    private Integer status;

    public String getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }

    public String getAnswerContent() {
        return answerContent;
    }

    public void setAnswerContent(String answerContent) {
        this.answerContent = answerContent;
    }

    public String getQuestionAttr() {
        return questionAttr;
    }

    public void setQuestionAttr(String questionAttr) {
        this.questionAttr = questionAttr;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
