package com.resqhub.model;

import com.resqhub.util.ValidationUtil;

/**
 * A single inventory resource (food packets, water bottles, medicines,
 * blankets, clothing, first-aid kits, rescue equipment, ...) tracked in
 * the Resource &amp; Inventory module.
 *
 * availableQuantity is the current stock level; minimumLevel is the
 * re-order / low-stock threshold. The {@link ResourceStatus} is derived
 * from these two (see {@link #deriveStatus()}).
 */
public class Resource extends BaseEntity {

    private String name;
    private String code;
    private ResourceCategory category;
    private int availableQuantity;
    private int minimumLevel;
    private String unit;
    private String description;
    private Long createdBy;

    public Resource() {
        super();
    }

    @Override
    public String getDetails() {
        return name + " [" + (category == null ? "?" : category.getLabel())
                + "] " + availableQuantity + " " + (unit == null ? "" : unit)
                + " / min " + minimumLevel
                + " (" + (status() == null ? "?" : status().getLabel()) + ")";
    }

    /** Computes the availability status from the current level. */
    public ResourceStatus status() {
        if (availableQuantity <= 0) {
            return ResourceStatus.OUT_OF_STOCK;
        }
        if (availableQuantity < minimumLevel) {
            return ResourceStatus.LOW_STOCK;
        }
        return ResourceStatus.AVAILABLE;
    }

    public boolean isLowOnStock() {
        return status() != ResourceStatus.AVAILABLE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ValidationUtil.clean(name);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = ValidationUtil.clean(code);
    }

    public ResourceCategory getCategory() {
        return category;
    }

    public void setCategory(ResourceCategory category) {
        this.category = category;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getMinimumLevel() {
        return minimumLevel;
    }

    public void setMinimumLevel(int minimumLevel) {
        this.minimumLevel = minimumLevel;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = ValidationUtil.clean(unit);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = ValidationUtil.clean(description);
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
