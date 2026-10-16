package com.resqhub.service.impl;

import com.resqhub.dao.DisasterDao;
import com.resqhub.dao.VictimDao;
import com.resqhub.dto.VictimFormDto;
import com.resqhub.model.RescueStatus;
import com.resqhub.model.Victim;
import com.resqhub.service.ActivityLogService;
import com.resqhub.service.VictimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
public class VictimServiceImpl implements VictimService {

    private final VictimDao victimDao;
    private final DisasterDao disasterDao;
    private final ActivityLogService activityLogService;

    @Autowired
    public VictimServiceImpl(VictimDao victimDao, DisasterDao disasterDao, ActivityLogService activityLogService) {
        this.victimDao = victimDao;
        this.disasterDao = disasterDao;
        this.activityLogService = activityLogService;
    }

    private synchronized String generateReferenceNumber() {
        int seq = victimDao.getNextSequence();
        return String.format("VIC-%d-%04d", Year.now().getValue(), seq);
    }

    @Override
    @Transactional
    public Long registerVictim(VictimFormDto dto, Long userId) {
        disasterDao.findById(dto.getDisasterId())
                .orElseThrow(() -> new IllegalArgumentException("Disaster record not found with ID: " + dto.getDisasterId()));

        Victim victim = new Victim();
        victim.setReferenceNumber(generateReferenceNumber());
        victim.setDisasterId(dto.getDisasterId());
        victim.setFullName(dto.getFullName().trim());
        victim.setAge(dto.getAge());
        victim.setContactNumber(dto.getContactNumber() != null ? dto.getContactNumber().trim() : null);
        victim.setLocation(dto.getLocation().trim());
        victim.setAffectedFamilyMembers(dto.getAffectedFamilyMembers() != null ? dto.getAffectedFamilyMembers() : 1);
        victim.setAccessibilityNeeds(dto.getAccessibilityNeeds() != null ? dto.getAccessibilityNeeds().trim() : null);
        victim.setCurrentSituation(dto.getCurrentSituation() != null ? dto.getCurrentSituation().trim() : null);
        victim.setRescueStatus(dto.getRescueStatus() != null ? dto.getRescueStatus() : RescueStatus.AWAITING_RESCUE);
        victim.setPriority(dto.getPriority());

        Long newId = victimDao.save(victim);

        activityLogService.log(userId, "REGISTER_VICTIM", "VICTIM", victim.getReferenceNumber(),
                "Registered victim " + victim.getFullName() + " (" + victim.getReferenceNumber() + ") with priority " + victim.getPriority().name());

        return newId;
    }

    @Override
    @Transactional
    public void updateVictim(VictimFormDto dto, Long userId) {
        Victim existing = victimDao.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Victim record not found with ID: " + dto.getId()));

        disasterDao.findById(dto.getDisasterId())
                .orElseThrow(() -> new IllegalArgumentException("Disaster record not found with ID: " + dto.getDisasterId()));

        existing.setDisasterId(dto.getDisasterId());
        existing.setFullName(dto.getFullName().trim());
        existing.setAge(dto.getAge());
        existing.setContactNumber(dto.getContactNumber() != null ? dto.getContactNumber().trim() : null);
        existing.setLocation(dto.getLocation().trim());
        existing.setAffectedFamilyMembers(dto.getAffectedFamilyMembers() != null ? dto.getAffectedFamilyMembers() : 1);
        existing.setAccessibilityNeeds(dto.getAccessibilityNeeds() != null ? dto.getAccessibilityNeeds().trim() : null);
        existing.setCurrentSituation(dto.getCurrentSituation() != null ? dto.getCurrentSituation().trim() : null);
        existing.setRescueStatus(dto.getRescueStatus());
        existing.setPriority(dto.getPriority());

        victimDao.update(existing);

        activityLogService.log(userId, "UPDATE_VICTIM", "VICTIM", existing.getReferenceNumber(),
                "Updated details for victim " + existing.getFullName() + " (" + existing.getReferenceNumber() + ")");
    }

    @Override
    @Transactional
    public void updateRescueStatus(Long victimId, RescueStatus status, Long userId) {
        Victim existing = victimDao.findById(victimId)
                .orElseThrow(() -> new IllegalArgumentException("Victim record not found with ID: " + victimId));

        RescueStatus oldStatus = existing.getRescueStatus();
        victimDao.updateStatus(victimId, status);

        activityLogService.log(userId, "UPDATE_RESCUE_STATUS", "VICTIM", existing.getReferenceNumber(),
                "Transitioned victim " + existing.getReferenceNumber() + " rescue status from " + oldStatus.name() + " to " + status.name());
    }

    @Override
    public Optional<Victim> getVictimById(Long id) {
        return victimDao.findById(id);
    }

    @Override
    public Optional<Victim> getVictimByReference(String referenceNumber) {
        return victimDao.findByReferenceNumber(referenceNumber);
    }

    @Override
    public List<Victim> getVictims(int page, int size, String search, Long disasterId, String status, String priority) {
        return victimDao.findAll(page, size, search, disasterId, status, priority);
    }

    @Override
    public int countVictims(String search, Long disasterId, String status, String priority) {
        return victimDao.count(search, disasterId, status, priority);
    }

    @Override
    public List<Victim> getAwaitingRescue(Long disasterId) {
        return victimDao.findAwaitingRescue(disasterId);
    }
}
