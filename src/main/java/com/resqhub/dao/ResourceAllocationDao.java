package com.resqhub.dao;

import com.resqhub.model.ResourceAllocation;
import com.resqhub.model.ResourceAllocationStatus;

import java.util.List;
import java.util.Optional;

public interface ResourceAllocationDao {
    Optional<ResourceAllocation> findById(Long id);
    List<ResourceAllocation> findAll();
    List<ResourceAllocation> findByDisasterId(Long disasterId);
    List<ResourceAllocation> findByShelterId(Long shelterId);
    List<ResourceAllocation> findByStatus(ResourceAllocationStatus status);
    Long save(ResourceAllocation allocation);
    void updateStatus(Long id, ResourceAllocationStatus status);
    long count();
}
