package com.okban.Enum;

public enum WayFlags {
    HIGHWAY(1),
    BUILDING(1 << 1),

    ONEWAY(1 << 2),

    ACCESS_NO(1 << 3),
    VEHICLE_NO(1 << 4),

    MOTORCAR_NO(1 << 5),
    MOTORCYCLE_NO(1 << 6),
    BICYCLE_NO(1 << 7),
    FOOT_NO(1 << 8),
    PSV_NO(1 << 16),

    FOOTWAY(1 << 9),

    DESTINATION(1 << 10),
    PRIVATE(1 << 11),

    HISTORIC(1 << 12),
    TOURISM(1 << 13),
    SERVICE(1 << 14),
    FEE(1 << 15);

    private int value;

    private WayFlags(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
