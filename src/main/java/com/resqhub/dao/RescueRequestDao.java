package com.resqhub.dao;

import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueRequestStatus;

import java.util.List;
import java.util.Optional;

public interface RescueRequestDao {

    Optional<RescueRequest> findById(Long id);

    Optional<RescueRequest> findByRequestNumber(String requestNumber);

    Long save(RescueRequest request);

    void update(RescueRequest request);

    void updateStatus(Long id, RescueRequestStatus status);

    List<RescueRequest> findAll(int page, int size, String search, Long disasterId, String status, String priority);

    int count(String search, Long disasterId, String status, String priority);

    List<RescueRequest> findByReportedUserId(Long userId, int page, int size);

    int countByReportedUserId(Long userId);

    int getNextSequence();

    boolean hasActiveAssignment(Long requestId);
}
