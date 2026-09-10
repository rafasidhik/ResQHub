package com.resqhub.model;

import java.time.LocalDateTime;

/**
 * An immutable event in a blood request's lifecycle (spec: Request History).
 * Records what happened, when, and who performed it, providing full
 * traceability from creation through donation to fulfilment.
 */
public class RequestHistory extends BaseEntity {

    private Long requestId;
    private String event;
    private String details;
    private Long performedBy;
    private LocalDateTime performedAt;

    public RequestHistory() {
        super();
    }

    @Override
    public String getDetails() {
        return event + (details == null || details.isEmpty()
                ? "" : " - " + details);
    }

    public String getRemarks() { return details; }
    public void setRemarks(String details) { this.details = details; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public Long getPerformedBy() { return performedBy; }
    public void setPerformedBy(Long performedBy) { this.performedBy = performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}
