package com.okban.model;

import java.util.ArrayList;
import java.util.List;

public class GraphNode {
    int id;
    double x;
    double y;
    double lon;
    double lat;
    List<Edge> edges;

    public GraphNode(int id, double x, double y, double lon, double lat) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.lon = lon;
        this.lat = lat;
    }

    public int getID() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public List<Edge> getEdges() {
        if (edges == null)
            edges = new ArrayList<>(2);
        return edges;
    }

}
