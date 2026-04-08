package com.okban.config;

import com.okban.Enum.VehicleType;

public class MapConfig {
    public final double TILE_WIDTH = 256;
    public final double TILE_HEIGHT = 256;

    public final double worldWidth = 100_000;
    public final double worldHeight = 100_000;

    public final double minLon = 106.30;
    public final double maxLon = 107.10;
    public final double minLat = 10.30;
    public final double maxLat = 11.20;

    public final double BUFFER = 512 * 2;

    public static VehicleType currentVehicleType = VehicleType.CAR;
    public static boolean snapcontextsChanged = true;
}
