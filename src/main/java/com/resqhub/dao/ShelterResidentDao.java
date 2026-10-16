package com.resqhub.dao;

import com.resqhub.model.ResidentStatus;
import com.resqhub.model.ShelterResident;

import java.util.List;
import java.util.Optional;

public interface ShelterResidentDao {
    Optional<ShelterResident> findById(Long id);
    List<ShelterResident> findByShelterId(Long shelterId);
    List<ShelterResident> findActiveByShelterId(Long shelterId);
    List<ShelterResident> findByDisasterId(Long disasterId);
    List<ShelterResident> findAll();
    Long save(ShelterResident resident);
    void checkOut(Long residentId);
    int countActiveResidentsByShelterId(Long shelterId);
    int sumActiveFamilySizeByShelterId(Long shelterId);
}
