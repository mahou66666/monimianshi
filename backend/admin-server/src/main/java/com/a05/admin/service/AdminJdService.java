package com.a05.admin.service;

import com.a05.admin.controller.dto.AdminJdQuery;

import java.util.List;
import java.util.Map;

public interface AdminJdService {

    Map<String, Object> listJobs(AdminJdQuery query);

    Map<String, Object> listGuideJobs(AdminJdQuery query);

    Map<String, Object> getLatestGuide(Long jdId);

    Map<String, Object> clearGuide(Long jdId);

    Map<String, Object> importTextBatch(String text, Long companyId);

    Map<String, Object> extractBatch(List<Long> jdIds);
}
