package com.resqhub.dao;

import com.resqhub.model.RescueTeam;
import com.resqhub.model.RescueTeamMember;
import com.resqhub.model.TeamAvailability;

import java.util.List;
import java.util.Optional;

public interface RescueTeamDao {

    Optional<RescueTeam> findById(Long id);

    Optional<RescueTeam> findByIdentifier(String identifier);

    Long save(RescueTeam team);

    void update(RescueTeam team);

    void updateAvailability(Long id, TeamAvailability availability);

    List<RescueTeam> findAll(int page, int size, String search, String availability, String status);

    int count(String search, String availability, String status);

    List<RescueTeam> findAvailable();

    List<RescueTeamMember> findMembersByTeamId(Long teamId);

    void saveMember(RescueTeamMember member);

    void deleteMember(Long memberId);

    int getNextSequence();
}
