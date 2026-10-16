package com.resqhub.dao;

import com.resqhub.model.RescueStatus;
import com.resqhub.model.Victim;

import java.util.List;
import java.util.Optional;

public interface VictimDao {

    Optional<Victim> findById(Long id);

    Optional<Victim> findByReferenceNumber(String referenceNumber);

    Long save(Victim victim);

    void update(Victim victim);

    void updateStatus(Long id, RescueStatus status);

    List<Victim> findAll(int page, int size, String search, Long disasterId, String status, String priority);

    int count(String search, Long disasterId, String status, String priority);

    List<Victim> findAwaitingRescue(Long disasterId);

    int getNextSequence();
}
