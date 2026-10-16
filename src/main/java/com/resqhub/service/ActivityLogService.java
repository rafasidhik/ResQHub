package com.resqhub.service;

import java.util.List;
import java.util.Map;

public interface ActivityLogService {

    void log(Long userId, String action, String module, String affectedRecordId, String description);

    List<Map<String, Object>> getRecentLogs(int limit);

    List<Map<String, Object>> getUserLogs(Long userId, int limit);

    List<Map<String, Object>> getAllLogs(int page, int size, String moduleFilter, String searchQuery);

    int countLogs(String moduleFilter, String searchQuery);
}
