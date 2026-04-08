package com.okban.Enum;

public enum HighwayType {
    MOTORWAY(13, "#E64A19"), // đỏ cam đậm
    MOTORWAY_LINK(12, "#FF7043"),

    TRUNK(13, "#FB8C00"), // cam
    TRUNK_LINK(12, "#FFA726"),

    PRIMARY(11, "#FDD835"), // vàng đậm
    PRIMARY_LINK(10, "#FFF176"),

    SECONDARY(9, "#90CAF9"), // xanh nhạt
    SECONDARY_LINK(8, "#BBDEFB"),

    TERTIARY(8, "#B0BEC5"), // xám xanh
    TERTIARY_LINK(7, "#CFD8DC"),

    RESIDENTIAL(7, "#E0E0E0"), // xám nhạt
    UNCLASSIFIED(7, "#E0E0E0"),
    SERVICE(3, "#EEEEEE"),
    LIVING_STREET(1, "#F5F5F5"),

    TRACK(5, "#A1887F"), // nâu
    PATH(4, "#9E9E9E"),

    FOOTWAY(1, "#43A047"), // xanh lá
    PEDESTRIAN(3, "#66BB6A"),
    CYCLEWAY(3, "#00C853"),
    STEPS(2, "#757575"),

    UNKNOWN(3, "#BDBDBD");

    private final double width;
    private final String hexColor;

    HighwayType(double width, String hexColor) {
        this.width = width;
        this.hexColor = hexColor;
    }

    public double getWidth() {
        return width;
    }

    public String getHexColor() {
        return hexColor;
    }
}
