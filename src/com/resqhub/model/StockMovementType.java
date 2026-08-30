package com.resqhub.model;

/** stock_movements.movement_type column - direction of an inventory
 *  quantity change. All changes go through either a stock-in (receive)
 *  or stock-out (distribute/use) transaction so history stays clear. */
public enum StockMovementType {
    STOCK_IN("Stock In"),
    STOCK_OUT("Stock Out");

    private final String label;

    StockMovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
