package com.okban.model;

public class POI {
    String name;
    double x;
    double y;

    public POI(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;

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

}
