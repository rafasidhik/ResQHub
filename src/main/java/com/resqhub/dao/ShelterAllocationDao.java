package com.resqhub.dao;

import com.resqhub.model.AllocationStatus;
import com.resqhub.model.ShelterAllocation;

import java.util.List;
import java.util.Optional;

public interface ShelterAllocationDao {
    Optional<ShelterAllocation> findById(Long id);
    List<ShelterAllocation> findByShelterId(Long shelterId);
    List<ShelterAllocation> findByDisasterId(Long disasterId);
    List<ShelterAllocation> findAll();
    Long save(ShelterAllocation allocation);
    void updateStatus(Long id, AllocationStatus status, Long decidedByUserId);
}
