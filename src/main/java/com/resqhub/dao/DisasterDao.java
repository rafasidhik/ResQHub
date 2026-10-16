package com.resqhub.dao;

import com.resqhub.model.Disaster;

import java.util.List;
import java.util.Optional;

public interface DisasterDao {

    Optional<Disaster> findById(Long id);

    Long save(Disaster disaster);

    void update(Disaster disaster);

    void delete(Long id);

    List<Disaster> findAll(int page, int size, String search, String status, String severity, String type);

    int count(String search, String status, String severity, String type);

    List<Disaster> findActive();

    int countRelatedVictims(Long disasterId);

    int countRelatedRequests(Long disasterId);

    int countRelatedShelterResidents(Long disasterId);

    int countRelatedResourceAllocations(Long disasterId);
}
