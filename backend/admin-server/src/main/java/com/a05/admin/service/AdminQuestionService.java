package com.a05.admin.service;

import com.a05.admin.controller.dto.GenerateGeneralQuestionsRequest;
import com.a05.admin.controller.dto.GeneralQuestionQuery;
import com.a05.admin.controller.dto.UpdateGeneralQuestionRequest;

import java.util.Map;

public interface AdminQuestionService {

    Map<String, Object> listQuestions(String category, GeneralQuestionQuery query);

    Map<String, Object> generateQuestions(String category, GenerateGeneralQuestionsRequest request);

    Map<String, Object> updateQuestion(String category, Long id, UpdateGeneralQuestionRequest request);

    Map<String, Object> deleteQuestion(String category, Long id);
}
