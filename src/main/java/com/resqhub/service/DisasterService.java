package com.resqhub.service;

import com.resqhub.dto.DisasterFormDto;
import com.resqhub.model.Disaster;

import java.util.List;
import java.util.Optional;

public interface DisasterService {

    Long createDisaster(DisasterFormDto dto, Long userId);

    void updateDisaster(DisasterFormDto dto, Long userId);

    void deleteDisaster(Long id, Long userId);

    Optional<Disaster> getDisasterById(Long id);

    List<Disaster> getDisasters(int page, int size, String search, String status, String severity, String type);

    int countDisasters(String search, String status, String severity, String type);

    List<Disaster> getActiveDisasters();
}
