package com.okban.Enum;

public enum WayFlags {
    HIGHWAY(1), BUILDING(2), ONEWAY(4), FOOTWAY(8), MOTOR_VEHICLE(16);

    private int value;

    private WayFlags(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
