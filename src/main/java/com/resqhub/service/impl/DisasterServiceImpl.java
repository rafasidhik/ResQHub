package com.resqhub.service.impl;

import com.resqhub.dao.DisasterDao;
import com.resqhub.dto.DisasterFormDto;
import com.resqhub.model.Disaster;
import com.resqhub.service.ActivityLogService;
import com.resqhub.service.DisasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DisasterServiceImpl implements DisasterService {

    private final DisasterDao disasterDao;
    private final ActivityLogService activityLogService;

    @Autowired
    public DisasterServiceImpl(DisasterDao disasterDao, ActivityLogService activityLogService) {
        this.disasterDao = disasterDao;
        this.activityLogService = activityLogService;
    }

    @Override
    @Transactional
    public Long createDisaster(DisasterFormDto dto, Long userId) {
        Disaster disaster = new Disaster();
        disaster.setName(dto.getName().trim());
        disaster.setDisasterType(dto.getDisasterType());
        disaster.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        disaster.setLocation(dto.getLocation().trim());
        disaster.setSeverity(dto.getSeverity());
        disaster.setStatus(dto.getStatus());
        disaster.setStartDate(dto.getStartDate());

        Long newId = disasterDao.save(disaster);

        activityLogService.log(userId, "CREATE_DISASTER", "DISASTER", String.valueOf(newId),
                "Declared disaster incident: " + disaster.getName() + " (" + disaster.getSeverity().name() + ")");

        return newId;
    }

    @Override
    @Transactional
    public void updateDisaster(DisasterFormDto dto, Long userId) {
        Disaster existing = disasterDao.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Disaster record not found with ID: " + dto.getId()));

        existing.setName(dto.getName().trim());
        existing.setDisasterType(dto.getDisasterType());
        existing.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        existing.setLocation(dto.getLocation().trim());
        existing.setSeverity(dto.getSeverity());
        existing.setStatus(dto.getStatus());
        existing.setStartDate(dto.getStartDate());

        disasterDao.update(existing);

        activityLogService.log(userId, "UPDATE_DISASTER", "DISASTER", String.valueOf(existing.getId()),
                "Updated disaster incident: " + existing.getName() + " [Status: " + existing.getStatus().name() + "]");
    }

    @Override
    @Transactional
    public void deleteDisaster(Long id, Long userId) {
        Disaster disaster = disasterDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disaster record not found with ID: " + id));

        // Safety enforcement: Prevent deletion if operational records exist
        int victimCount = disasterDao.countRelatedVictims(id);
        int requestCount = disasterDao.countRelatedRequests(id);
        int residentCount = disasterDao.countRelatedShelterResidents(id);
        int resourceCount = disasterDao.countRelatedResourceAllocations(id);

        if (victimCount > 0 || requestCount > 0 || residentCount > 0 || resourceCount > 0) {
            throw new IllegalStateException(String.format(
                    "Cannot delete disaster '%s'. It has active associated records: " +
                    "%d victims, %d rescue requests, %d shelter residents, and %d resource allocations.",
                    disaster.getName(), victimCount, requestCount, residentCount, resourceCount));
        }

        disasterDao.delete(id);

        activityLogService.log(userId, "DELETE_DISASTER", "DISASTER", String.valueOf(id),
                "Removed empty disaster incident: " + disaster.getName());
    }

    @Override
    public Optional<Disaster> getDisasterById(Long id) {
        return disasterDao.findById(id);
    }

    @Override
    public List<Disaster> getDisasters(int page, int size, String search, String status, String severity, String type) {
        return disasterDao.findAll(page, size, search, status, severity, type);
    }

    @Override
    public int countDisasters(String search, String status, String severity, String type) {
        return disasterDao.count(search, status, severity, type);
    }

    @Override
    public List<Disaster> getActiveDisasters() {
        return disasterDao.findActive();
    }
}
