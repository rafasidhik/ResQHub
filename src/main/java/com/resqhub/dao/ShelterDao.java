package com.resqhub.dao;

import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterStatus;

import java.util.List;
import java.util.Optional;

public interface ShelterDao {
    Optional<Shelter> findById(Long id);
    Optional<Shelter> findByIdentifier(String identifier);
    List<Shelter> findAll();
    List<Shelter> findByStatus(ShelterStatus status);
    List<Shelter> findAvailableShelters();
    Long save(Shelter shelter);
    void update(Shelter shelter);
    void updateOccupancy(Long shelterId, int newOccupancy, ShelterStatus newStatus);
    void delete(Long id);
    long count();
    int getTotalCapacitySum();
    int getCurrentOccupancySum();
    String generateNextIdentifier();
}
