package com.resqhub.service.impl;

import com.resqhub.dao.ActivityLogDao;
import com.resqhub.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogDao activityLogDao;

    @Autowired
    public ActivityLogServiceImpl(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    @Override
    public void log(Long userId, String action, String module, String affectedRecordId, String description) {
        activityLogDao.log(userId, action, module, affectedRecordId, description);
    }

    @Override
    public List<Map<String, Object>> getRecentLogs(int limit) {
        return activityLogDao.findRecent(limit);
    }

    @Override
    public List<Map<String, Object>> getUserLogs(Long userId, int limit) {
        return activityLogDao.findByUserId(userId, limit);
    }

    @Override
    public List<Map<String, Object>> getAllLogs(int page, int size, String moduleFilter, String searchQuery) {
        return activityLogDao.findAll(page, size, moduleFilter, searchQuery);
    }

    @Override
    public int countLogs(String moduleFilter, String searchQuery) {
        return activityLogDao.count(moduleFilter, searchQuery);
    }
}
