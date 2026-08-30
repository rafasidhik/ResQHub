package com.resqhub.model;

import java.time.LocalDateTime;

/**
 * Help request raised by a victim or citizen.
 * The boolean flags and counts feed the Rescue Priority Algorithm;
 * priority is null until the engine computes it.
 */
public class RescueRequest extends BaseEntity {

    private Long disasterId;
    private Long victimId;               // null if caller not registered
    private String requesterName;
    private String contactNumber;
    private String location;
    private int peopleCount = 1;
    private int childrenCount;
    private int elderlyCount;
    private boolean lifeThreatening;
    private boolean medicalEmergency;
    private boolean trappedUnderDebris;
    private String requiredAssistance;
    private PriorityLevel priority;      // computed by service layer
    private RequestStatus status = RequestStatus.PENDING;
    private LocalDateTime requestedAt;

    public RescueRequest() {
        super();
    }

    public RescueRequest(Long disasterId, String requesterName,
                         String contactNumber, String location) {
        super();
        this.disasterId = disasterId;
        this.requesterName = requesterName;
        this.contactNumber = contactNumber;
        this.location = location;
        this.requestedAt = LocalDateTime.now();
    }

    @Override
    public String getDetails() {
        String prio = priority == null ? "UNRATED" : priority.getLabel();
        return requesterName + " x" + peopleCount + " (" + prio + ") @ "
                + (location == null ? "unknown" : location);
    }

    /** True when children or elderly are among the people needing rescue. */
    public boolean hasVulnerableOccupants() {
        return childrenCount > 0 || elderlyCount > 0;
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public Long getVictimId() {
        return victimId;
    }

    public void setVictimId(Long victimId) {
        this.victimId = victimId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(int peopleCount) {
        this.peopleCount = peopleCount;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public int getElderlyCount() {
        return elderlyCount;
    }

    public void setElderlyCount(int elderlyCount) {
        this.elderlyCount = elderlyCount;
    }

    public boolean isLifeThreatening() {
        return lifeThreatening;
    }

    public void setLifeThreatening(boolean lifeThreatening) {
        this.lifeThreatening = lifeThreatening;
    }

    public boolean isMedicalEmergency() {
        return medicalEmergency;
    }

    public void setMedicalEmergency(boolean medicalEmergency) {
        this.medicalEmergency = medicalEmergency;
    }

    public boolean isTrappedUnderDebris() {
        return trappedUnderDebris;
    }

    public void setTrappedUnderDebris(boolean trappedUnderDebris) {
        this.trappedUnderDebris = trappedUnderDebris;
    }

    public String getRequiredAssistance() {
        return requiredAssistance;
    }

    public void setRequiredAssistance(String requiredAssistance) {
        this.requiredAssistance = requiredAssistance;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
