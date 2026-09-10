package com.resqhub.model;

/**
 * Blood group (ABO + Rh). Encapsulates the ABO/Rh compatibility rules used
 * by donor matching so a donor's blood can safely serve a request's blood
 * group (spec: Blood Group Management / Donor Matching).
 */
public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True if this blood group is Rh positive. */
    public boolean isRhPositive() {
        return name().contains("POSITIVE");
    }

    /**
     * Can a donor of {@code this} group donate blood to a recipient of
     * {@code recipient} group, applying ABO + Rh compatibility rules?
     */
    public boolean canDonateTo(BloodGroup recipient) {
        if (recipient == null || this == null) {
            return false;
        }
        if (this == O_NEGATIVE) {
            // O- is the universal donor for all ABO + Rh combinations.
            return true;
        }
        if (this == O_POSITIVE) {
            // O+ can only go to Rh+ recipients of any ABO type.
            return recipient.isRhPositive();
        }
        if (this == AB_POSITIVE || this == AB_NEGATIVE) {
            // AB can donate to AB only (with matching Rh).
            if (recipient != AB_POSITIVE && recipient != AB_NEGATIVE) {
                return false;
            }
            // AB- can donate to AB- and AB+; AB+ can only donate to AB+.
            return !this.isRhPositive() || recipient.isRhPositive();
        }
        // A and B types: donate to own letter, plus AB (Rh permitting).
        boolean sameLetter = sameAboLetter(recipient)
                || recipient == AB_POSITIVE || recipient == AB_NEGATIVE;
        if (!sameLetter) {
            return false;
        }
        // Rh- can donate to Rh- and Rh+; Rh+ can only donate to Rh+.
        return !this.isRhPositive() || recipient.isRhPositive();
    }

    private boolean sameAboLetter(BloodGroup other) {
        return aboFamily(this) == aboFamily(other);
    }

    private int aboFamily(BloodGroup g) {
        if (g == A_POSITIVE || g == A_NEGATIVE) {
            return 1;
        }
        if (g == B_POSITIVE || g == B_NEGATIVE) {
            return 2;
        }
        if (g == AB_POSITIVE || g == AB_NEGATIVE) {
            return 3;
        }
        return 0;
    }

    /** Human-readable compatibility description for this donor group. */
    public String compatibleRecipients() {
        StringBuilder sb = new StringBuilder();
        for (BloodGroup g : values()) {
            if (canDonateTo(g)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(g.getLabel());
            }
        }
        return sb.toString();
    }
}
