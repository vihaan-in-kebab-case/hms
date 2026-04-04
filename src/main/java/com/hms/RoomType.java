package com.hms;
public enum RoomType {

    SINGLE    ("Single",    1800),
    DOUBLE    ("Double",    3200),
    DELUXE    ("Deluxe",    5500),
    SUITE     ("Suite",     9000),
    PENTHOUSE ("Penthouse", 18000);

    private final String displayName;
    private final double baseTariff;

    RoomType(String displayName, double baseTariff) {
        this.displayName = displayName;
        this.baseTariff  = baseTariff;
    }

    public String getDisplayName() { return displayName; }
    public double getBaseTariff()  { return baseTariff; }

    public double calculateCost(int nights) {
        return baseTariff * nights;
    }

    public double calculateCostWithTax(int nights) {
        return calculateCost(nights) * 1.12;
    }

    public static RoomType fromDisplayName(String name) {
        for (RoomType rt : values()) {
            if (rt.displayName.equalsIgnoreCase(name)) return rt;
        }
        return SINGLE;
    }

    @Override
    public String toString() { return displayName; }
}
