package com.resqhub.dao;

import java.util.List;
import java.util.Map;

public interface ActivityLogDao {

    void log(Long userId, String action, String module, String affectedRecordId, String description);

    List<Map<String, Object>> findRecent(int limit);

    List<Map<String, Object>> findByUserId(Long userId, int limit);

    List<Map<String, Object>> findAll(int page, int size, String moduleFilter, String searchQuery);

    int count(String moduleFilter, String searchQuery);
}
