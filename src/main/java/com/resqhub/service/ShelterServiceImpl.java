package com.resqhub.service;

import com.resqhub.dao.DisasterDao;
import com.resqhub.dao.ShelterAllocationDao;
import com.resqhub.dao.ShelterDao;
import com.resqhub.dao.ShelterResidentDao;
import com.resqhub.dao.VictimDao;
import com.resqhub.dto.ResidentCheckInDto;
import com.resqhub.dto.ShelterDto;
import com.resqhub.dto.ShelterRecommendation;
import com.resqhub.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ShelterServiceImpl implements ShelterService {

    private final ShelterDao shelterDao;
    private final ShelterResidentDao shelterResidentDao;
    private final ShelterAllocationDao shelterAllocationDao;
    private final DisasterDao disasterDao;
    private final VictimDao victimDao;
    private final ActivityLogService activityLogService;

    @Autowired
    public ShelterServiceImpl(ShelterDao shelterDao,
                              ShelterResidentDao shelterResidentDao,
                              ShelterAllocationDao shelterAllocationDao,
                              DisasterDao disasterDao,
                              VictimDao victimDao,
                              ActivityLogService activityLogService) {
        this.shelterDao = shelterDao;
        this.shelterResidentDao = shelterResidentDao;
        this.shelterAllocationDao = shelterAllocationDao;
        this.disasterDao = disasterDao;
        this.victimDao = victimDao;
        this.activityLogService = activityLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shelter> getAllShelters() {
        return shelterDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Shelter> getShelterById(Long id) {
        return shelterDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Shelter> getShelterByIdentifier(String identifier) {
        return shelterDao.findByIdentifier(identifier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shelter> getAvailableShelters() {
        return shelterDao.findAvailableShelters();
    }

    @Override
    public Shelter createShelter(ShelterDto dto, Long currentUserId) {
        Shelter shelter = new Shelter();
        shelter.setShelterIdentifier(shelterDao.generateNextIdentifier());
        shelter.setName(dto.getName());
        shelter.setAddress(dto.getAddress());
        shelter.setLocationDescription(dto.getLocationDescription());
        shelter.setContactPerson(dto.getContactPerson());
        shelter.setContactNumber(dto.getContactNumber());
        shelter.setTotalCapacity(dto.getTotalCapacity());
        shelter.setCurrentOccupancy(0);
        shelter.setStatus(dto.getStatus() != null ? dto.getStatus() : ShelterStatus.AVAILABLE);

        Long id = shelterDao.save(shelter);
        shelter.setId(id);

        activityLogService.log(currentUserId, "CREATE_SHELTER", "SHELTERS",
                shelter.getShelterIdentifier(), "Registered new shelter: " + shelter.getName() + " with capacity " + shelter.getTotalCapacity());

        return shelter;
    }

    @Override
    public Shelter updateShelter(Long id, ShelterDto dto, Long currentUserId) {
        Shelter shelter = shelterDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + id));

        shelter.setName(dto.getName());
        shelter.setAddress(dto.getAddress());
        shelter.setLocationDescription(dto.getLocationDescription());
        shelter.setContactPerson(dto.getContactPerson());
        shelter.setContactNumber(dto.getContactNumber());
        shelter.setTotalCapacity(dto.getTotalCapacity());

        if (dto.getStatus() != null) {
            shelter.setStatus(dto.getStatus());
        }

        // Recalculate status according to occupancy unless temporarily closed
        recalculateAndSaveOccupancy(shelter);
        shelterDao.update(shelter);

        activityLogService.log(currentUserId, "UPDATE_SHELTER", "SHELTERS",
                shelter.getShelterIdentifier(), "Updated shelter information for: " + shelter.getName());

        return shelter;
    }

    @Override
    public void deleteShelter(Long id, Long currentUserId) {
        Shelter shelter = shelterDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + id));

        int activeResidents = shelterResidentDao.countActiveResidentsByShelterId(id);
        if (activeResidents > 0) {
            throw new IllegalStateException("Cannot delete shelter '" + shelter.getName() + "' because it currently houses " + activeResidents + " active resident(s).");
        }

        shelterDao.delete(id);
        activityLogService.log(currentUserId, "DELETE_SHELTER", "SHELTERS",
                shelter.getShelterIdentifier(), "Deleted shelter: " + shelter.getName());
    }

    @Override
    public ShelterResident checkInResident(ResidentCheckInDto dto, Long currentUserId) {
        Shelter shelter = shelterDao.findById(dto.getShelterId())
                .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + dto.getShelterId()));

        if (shelter.getStatus() == ShelterStatus.TEMPORARILY_CLOSED) {
            throw new IllegalStateException("Cannot check in residents. Shelter '" + shelter.getName() + "' is temporarily closed.");
        }

        int remaining = shelter.getRemainingCapacity();
        if (remaining < dto.getFamilySize()) {
            throw new IllegalStateException("Insufficient capacity at '" + shelter.getName() + "'. Available: " + remaining + ", Requested: " + dto.getFamilySize());
        }

        ShelterResident resident = new ShelterResident();
        resident.setShelterId(shelter.getId());
        resident.setDisasterId(dto.getDisasterId());
        resident.setVictimId(dto.getVictimId());
        resident.setResidentName(dto.getResidentName());
        resident.setFamilySize(dto.getFamilySize());
        resident.setStatus(ResidentStatus.ACTIVE);
        resident.setRegisteredByUserId(currentUserId);

        Long residentId = shelterResidentDao.save(resident);
        resident.setId(residentId);

        // Update shelter occupancy
        recalculateAndSaveOccupancy(shelter);

        activityLogService.log(currentUserId, "CHECK_IN_RESIDENT", "SHELTERS",
                String.valueOf(residentId), "Checked in " + resident.getResidentName() + " (family of " + resident.getFamilySize() + ") into " + shelter.getName());

        return resident;
    }

    @Override
    public void checkOutResident(Long residentId, Long currentUserId) {
        ShelterResident resident = shelterResidentDao.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found with id: " + residentId));

        if (resident.getStatus() == ResidentStatus.CHECKED_OUT) {
            return; // Already checked out
        }

        shelterResidentDao.checkOut(residentId);

        // Recalculate shelter occupancy
        Shelter shelter = shelterDao.findById(resident.getShelterId())
                .orElseThrow(() -> new IllegalStateException("Shelter not found for resident"));
        recalculateAndSaveOccupancy(shelter);

        activityLogService.log(currentUserId, "CHECK_OUT_RESIDENT", "SHELTERS",
                String.valueOf(residentId), "Checked out " + resident.getResidentName() + " from " + shelter.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelterResident> getResidentsByShelter(Long shelterId) {
        return shelterResidentDao.findByShelterId(shelterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelterResident> getActiveResidentsByShelter(Long shelterId) {
        return shelterResidentDao.findActiveByShelterId(shelterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelterRecommendation> recommendShelters(Long disasterId, int peopleCount) {
        Disaster disaster = disasterDao.findById(disasterId)
                .orElseThrow(() -> new IllegalArgumentException("Disaster not found with id: " + disasterId));

        List<Shelter> allShelters = shelterDao.findAll();
        List<ShelterRecommendation> recommendations = new ArrayList<>();

        String disasterLocation = (disaster.getLocation() != null ? disaster.getLocation().toLowerCase() : "");

        for (Shelter shelter : allShelters) {
            if (shelter.getStatus() == ShelterStatus.TEMPORARILY_CLOSED) {
                continue;
            }

            int remaining = shelter.getRemainingCapacity();
            if (remaining < peopleCount) {
                continue; // Cannot fit the requested group
            }

            int score = 0;
            StringBuilder reasons = new StringBuilder();

            // 1. Capacity Buffer scoring (up to 40 pts)
            int postOccupancy = shelter.getCurrentOccupancy() + peopleCount;
            int projectedPct = shelter.getTotalCapacity() > 0 
                    ? (int) Math.round(((double) postOccupancy / shelter.getTotalCapacity()) * 100)
                    : 100;

            if (projectedPct <= 70) {
                score += 40;
                reasons.append("Ample capacity buffer (projected ").append(projectedPct).append("%). ");
            } else if (projectedPct <= 90) {
                score += 30;
                reasons.append("Moderate capacity buffer (projected ").append(projectedPct).append("%). ");
            } else {
                score += 15;
                reasons.append("Tight fit near capacity (projected ").append(projectedPct).append("%). ");
            }

            // 2. Status scoring (up to 30 pts)
            if (shelter.getStatus() == ShelterStatus.AVAILABLE) {
                score += 30;
                reasons.append("Status is fully Available. ");
            } else if (shelter.getStatus() == ShelterStatus.NEARLY_FULL) {
                score += 15;
                reasons.append("Status is Nearly Full. ");
            }

            // 3. Proximity / Location affinity (up to 30 pts)
            String address = (shelter.getAddress() != null ? shelter.getAddress().toLowerCase() : "");
            String locationDesc = (shelter.getLocationDescription() != null ? shelter.getLocationDescription().toLowerCase() : "");

            boolean locationMatch = false;
            if (!disasterLocation.isEmpty()) {
                String[] words = disasterLocation.split("[,\\s]+");
                for (String word : words) {
                    if (word.length() > 3 && (address.contains(word) || locationDesc.contains(word))) {
                        locationMatch = true;
                        break;
                    }
                }
            }

            if (locationMatch) {
                score += 30;
                reasons.append("Proximity match to disaster area. ");
            } else {
                score += 15;
                reasons.append("General operational shelter. ");
            }

            recommendations.add(new ShelterRecommendation(
                    shelter,
                    score,
                    reasons.toString().trim(),
                    remaining,
                    projectedPct
            ));
        }

        // Sort descending by score, then by remaining capacity
        recommendations.sort(
                Comparator.comparingInt(ShelterRecommendation::getScore).reversed()
                        .thenComparing(Comparator.comparingInt(ShelterRecommendation::getAvailableCapacity).reversed())
        );

        return recommendations;
    }

    @Override
    public ShelterAllocation createAllocation(Long disasterId, Long shelterId, int peopleCount, String reason, Long currentUserId) {
        Shelter shelter = shelterDao.findById(shelterId)
                .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + shelterId));
        disasterDao.findById(disasterId)
                .orElseThrow(() -> new IllegalArgumentException("Disaster not found with id: " + disasterId));

        ShelterAllocation allocation = new ShelterAllocation();
        allocation.setShelterId(shelterId);
        allocation.setDisasterId(disasterId);
        allocation.setRequestedPeopleCount(peopleCount);
        allocation.setStatus(AllocationStatus.RECOMMENDED);
        allocation.setRecommendationReason(reason);
        allocation.setDecidedByUserId(currentUserId);

        Long allocId = shelterAllocationDao.save(allocation);
        allocation.setId(allocId);

        activityLogService.log(currentUserId, "ALLOCATE_SHELTER", "SHELTERS",
                String.valueOf(allocId), "Created shelter allocation recommendation for " + peopleCount + " people to " + shelter.getName());

        return allocation;
    }

    @Override
    public void confirmAllocation(Long allocationId, Long currentUserId) {
        ShelterAllocation allocation = shelterAllocationDao.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with id: " + allocationId));

        shelterAllocationDao.updateStatus(allocationId, AllocationStatus.CONFIRMED, currentUserId);
        activityLogService.log(currentUserId, "CONFIRM_SHELTER_ALLOCATION", "SHELTERS",
                String.valueOf(allocationId), "Confirmed shelter allocation of " + allocation.getRequestedPeopleCount() + " people.");
    }

    @Override
    public void rejectAllocation(Long allocationId, Long currentUserId) {
        shelterAllocationDao.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with id: " + allocationId));

        shelterAllocationDao.updateStatus(allocationId, AllocationStatus.REJECTED, currentUserId);
        activityLogService.log(currentUserId, "REJECT_SHELTER_ALLOCATION", "SHELTERS",
                String.valueOf(allocationId), "Rejected shelter allocation recommendation.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelterAllocation> getAllocationsByDisaster(Long disasterId) {
        return shelterAllocationDao.findByDisasterId(disasterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelterAllocation> getAllAllocations() {
        return shelterAllocationDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalShelterCount() {
        return shelterDao.count();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalCapacity() {
        return shelterDao.getTotalCapacitySum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getCurrentOccupancy() {
        return shelterDao.getCurrentOccupancySum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getOverallOccupancyPercentage() {
        int total = getTotalCapacity();
        if (total <= 0) return 0;
        int current = getCurrentOccupancy();
        return Math.min(100, (int) Math.round(((double) current / total) * 100));
    }

    private void recalculateAndSaveOccupancy(Shelter shelter) {
        int activeOccupancy = shelterResidentDao.sumActiveFamilySizeByShelterId(shelter.getId());
        shelter.setCurrentOccupancy(activeOccupancy);

        // Keep TEMPORARILY_CLOSED if manually set, otherwise evaluate status
        if (shelter.getStatus() != ShelterStatus.TEMPORARILY_CLOSED) {
            if (activeOccupancy >= shelter.getTotalCapacity()) {
                shelter.setStatus(ShelterStatus.FULL);
            } else if (activeOccupancy >= (int) Math.round(shelter.getTotalCapacity() * 0.8)) {
                shelter.setStatus(ShelterStatus.NEARLY_FULL);
            } else {
                shelter.setStatus(ShelterStatus.AVAILABLE);
            }
        }

        shelterDao.updateOccupancy(shelter.getId(), shelter.getCurrentOccupancy(), shelter.getStatus());
    }
}
