package com.resqhub.dao;

import com.resqhub.model.DistributionStatus;
import com.resqhub.model.FoodDistribution;

import java.util.List;
import java.util.Optional;

public interface FoodDistributionDao {
    Optional<FoodDistribution> findById(Long id);
    List<FoodDistribution> findAll();
    List<FoodDistribution> findByShelterId(Long shelterId);
    List<FoodDistribution> findByStatus(DistributionStatus status);
    Long save(FoodDistribution distribution);
    void completeDistribution(Long id);
    long count();
    int sumRecipientsServed();
}
