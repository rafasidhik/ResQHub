package com.resqhub.service;

import com.resqhub.dto.VictimFormDto;
import com.resqhub.model.RescueStatus;
import com.resqhub.model.Victim;

import java.util.List;
import java.util.Optional;

public interface VictimService {

    Long registerVictim(VictimFormDto dto, Long userId);

    void updateVictim(VictimFormDto dto, Long userId);

    void updateRescueStatus(Long victimId, RescueStatus status, Long userId);

    Optional<Victim> getVictimById(Long id);

    Optional<Victim> getVictimByReference(String referenceNumber);

    List<Victim> getVictims(int page, int size, String search, Long disasterId, String status, String priority);

    int countVictims(String search, Long disasterId, String status, String priority);

    List<Victim> getAwaitingRescue(Long disasterId);
}
