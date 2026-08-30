package com.resqhub.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of a smart allocation evaluation: the ranked candidate
 * shelters, the single best match (when any exist) and the allocation
 * record once the best shelter has been confirmed.
 */
public class SmartAllocationResult {

    private final SmartAllocationRequest request;
    private final List<RankedShelter> ranked = new ArrayList<>();
    private RankedShelter best;
    private ShelterAllocation allocation;

    public SmartAllocationResult(SmartAllocationRequest request) {
        this.request = request;
    }

    public SmartAllocationRequest getRequest() {
        return request;
    }

    public List<RankedShelter> getRanked() {
        return ranked;
    }

    public void addRanked(RankedShelter r) {
        ranked.add(r);
    }

    public RankedShelter getBest() {
        return best;
    }

    public void setBest(RankedShelter best) {
        this.best = best;
    }

    public ShelterAllocation getAllocation() {
        return allocation;
    }

    public void setAllocation(ShelterAllocation allocation) {
        this.allocation = allocation;
    }
}
