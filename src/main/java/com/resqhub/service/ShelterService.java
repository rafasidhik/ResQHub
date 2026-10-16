package com.resqhub.service;

import com.resqhub.dto.ResidentCheckInDto;
import com.resqhub.dto.ShelterDto;
import com.resqhub.dto.ShelterRecommendation;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterResident;

import java.util.List;
import java.util.Optional;

public interface ShelterService {
    List<Shelter> getAllShelters();
    Optional<Shelter> getShelterById(Long id);
    Optional<Shelter> getShelterByIdentifier(String identifier);
    List<Shelter> getAvailableShelters();
    Shelter createShelter(ShelterDto dto, Long currentUserId);
    Shelter updateShelter(Long id, ShelterDto dto, Long currentUserId);
    void deleteShelter(Long id, Long currentUserId);

    ShelterResident checkInResident(ResidentCheckInDto dto, Long currentUserId);
    void checkOutResident(Long residentId, Long currentUserId);
    List<ShelterResident> getResidentsByShelter(Long shelterId);
    List<ShelterResident> getActiveResidentsByShelter(Long shelterId);

    List<ShelterRecommendation> recommendShelters(Long disasterId, int peopleCount);
    ShelterAllocation createAllocation(Long disasterId, Long shelterId, int peopleCount, String reason, Long currentUserId);
    void confirmAllocation(Long allocationId, Long currentUserId);
    void rejectAllocation(Long allocationId, Long currentUserId);
    List<ShelterAllocation> getAllocationsByDisaster(Long disasterId);
    List<ShelterAllocation> getAllAllocations();

    long getTotalShelterCount();
    int getTotalCapacity();
    int getCurrentOccupancy();
    int getOverallOccupancyPercentage();
}
