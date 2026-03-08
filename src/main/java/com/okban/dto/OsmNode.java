package com.okban.dto;

public class OsmNode {
    long id;
    double lat;
    double lon;

    public OsmNode(long id, double lon, double lat) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }

    public long getID() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}