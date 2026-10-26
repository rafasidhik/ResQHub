package com.resqhub.model;

/** An equipment record attached to a rescue team. */
public class TeamEquipment extends BaseEntity {

    private Long teamId;
    private String equipmentName;
    private int quantity;
    private String description;

    public TeamEquipment() {
        super();
    }

    public TeamEquipment(Long teamId, String equipmentName, int quantity) {
        super();
        this.teamId = teamId;
        this.equipmentName = equipmentName;
        this.quantity = quantity;
    }

    @Override
    public String getDetails() {
        return equipmentName + " x" + quantity;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
