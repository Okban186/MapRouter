package com.okban.Enum;

public enum HighwayType {
    MOTORWAY(13, "#E9967A"),
    TRUNK(13, "#F0AD4E"),
    PRIMARY(11, "#FFEEAD"),
    SECONDARY(9, "#F7F7F7"),
    TERTIARY(8, "#FFFFFF"),
    RESIDENTIAL(7, "#FFFFFF"),
    FOOTWAY(1, "#BDBDBD"),
    UNSET(3, "#BDBDBD");

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
