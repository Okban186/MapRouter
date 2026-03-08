package com.okban.model;

public class POI {
    String name;
    double x;
    double y;
    String type;

    public POI(String name, double x, double y, String type) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getType() {
        return type;
    }
}
