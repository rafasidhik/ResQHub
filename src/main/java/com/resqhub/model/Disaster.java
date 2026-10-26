package com.resqhub.model;

import java.time.LocalDateTime;

/** One disaster event. Disaster -> BaseEntity (hierarchical inheritance). */
public class Disaster extends BaseEntity {

    private String title;
    private DisasterType disasterType;
    private DisasterSeverity severity;
    private DisasterStatus status = DisasterStatus.REPORTED;
    private String location;
    private int affectedPopulation;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;   // null while ongoing
    private String description;
    private Long reportedBy;

    public Disaster() {
        super();
    }

    public Disaster(String title, DisasterType disasterType,
                    DisasterSeverity severity, String location,
                    LocalDateTime startDateTime) {
        super();
        this.title = title;
        this.disasterType = disasterType;
        this.severity = severity;
        this.location = location;
        this.startDateTime = startDateTime;
    }

    @Override
    public String getDetails() {
        String typeLabel = disasterType == null ? "?" : disasterType.getLabel();
        String sevLabel = severity == null ? "?" : severity.getLabel();
        return title + " [" + typeLabel + "/" + sevLabel + "] @ "
                + (location == null ? "unknown" : location);
    }

    public boolean isOngoing() {
        return status == DisasterStatus.REPORTED
                || status == DisasterStatus.ACTIVE
                || status == DisasterStatus.CONTAINED;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public DisasterSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(DisasterSeverity severity) {
        this.severity = severity;
    }

    public DisasterStatus getStatus() {
        return status;
    }

    public void setStatus(DisasterStatus status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getAffectedPopulation() {
        return affectedPopulation;
    }

    public void setAffectedPopulation(int affectedPopulation) {
        this.affectedPopulation = affectedPopulation;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(Long reportedBy) {
        this.reportedBy = reportedBy;
    }
}
