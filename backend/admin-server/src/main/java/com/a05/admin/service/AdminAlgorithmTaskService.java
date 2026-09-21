package com.a05.admin.service;

import java.util.Map;

public interface AdminAlgorithmTaskService {

    Map<String, Object> createTask(Long userId, String algoKey, Map<String, Object> request);

    Map<String, Object> getTask(Long userId, String taskId);

    Map<String, Object> cancelTask(Long userId, String taskId);

    Map<String, Object> getPendingResumeIds(Long userId, String taskId);

    Map<String, Object> getPendingResumeFileIds(Long userId, String taskId);
}
