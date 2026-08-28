package com.resqhub.model;

import java.time.LocalDateTime;

import com.resqhub.util.ValidationUtil;

/**
 * A food distribution request (spec section 1). Tracks who needs food,
 * how many people, how much food and the full request lifecycle from
 * PENDING through APPROVED / ALLOCATED / IN_PROGRESS to COMPLETED.
 *
 * Quantity picture (spec section 5):
 *   requiredQuantity  - the total food requested
 *   allocatedQuantity - how much has been reserved / approved
 *   distributedQuantity - derived from the distribution history records
 *   remainingQuantity - what is still owed (required - distributed)
 *
 * The estimated requirement can be calculated from the number of
 * beneficiaries (spec section 4) rather than typed in by hand.
 */
public class FoodDistributionRequest extends BaseEntity {

    private String requestCode;
    private Long disasterId;
    private String location;
    private BeneficiaryType beneficiaryType;
    private int beneficiaries;
    private int requiredQuantity;
    private PriorityLevel priority = PriorityLevel.MEDIUM;
    private FoodRequestStatus status = FoodRequestStatus.PENDING;
    private String description;
    private LocalDateTime requestedAt;
    private Long createdBy;

    // allocation info (spec sections 7, 13)
    private int allocatedQuantity;
    private Long allocatedResourceId;
    private LocalDateTime allocatedAt;
    private Long allocatedBy;

    private Long assignedVolunteerId;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    /** Total handed out across the distribution history (transient, set
     *  by the DAO/service from the resource_distributions of this request). */
    private int distributedQuantity;

    public FoodDistributionRequest() {
        super();
    }

    @Override
    public String getDetails() {
        return requestCode + " @" + (location == null ? "?" : location)
                + " req " + requiredQuantity + " [" + (status == null
                        ? "?" : status.getLabel()) + "]";
    }

    /** Food still owed to the beneficiaries (required - distributed). */
    public int remainingQuantity() {
        return Math.max(0, requiredQuantity - distributedQuantity);
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = ValidationUtil.clean(requestCode);
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = ValidationUtil.clean(location);
    }

    public BeneficiaryType getBeneficiaryType() {
        return beneficiaryType;
    }

    public void setBeneficiaryType(BeneficiaryType beneficiaryType) {
        this.beneficiaryType = beneficiaryType;
    }

    public int getBeneficiaries() {
        return beneficiaries;
    }

    public void setBeneficiaries(int beneficiaries) {
        this.beneficiaries = beneficiaries;
    }

    public int getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(int requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public FoodRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FoodRequestStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = ValidationUtil.clean(description);
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public int getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(int allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public Long getAllocatedResourceId() {
        return allocatedResourceId;
    }

    public void setAllocatedResourceId(Long allocatedResourceId) {
        this.allocatedResourceId = allocatedResourceId;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    public Long getAllocatedBy() {
        return allocatedBy;
    }

    public void setAllocatedBy(Long allocatedBy) {
        this.allocatedBy = allocatedBy;
    }

    public Long getAssignedVolunteerId() {
        return assignedVolunteerId;
    }

    public void setAssignedVolunteerId(Long assignedVolunteerId) {
        this.assignedVolunteerId = assignedVolunteerId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getDistributedQuantity() {
        return distributedQuantity;
    }

    public void setDistributedQuantity(int distributedQuantity) {
        this.distributedQuantity = distributedQuantity;
    }
}
