package com.okban.model;

public class SnapResult {

    int node1;
    int node2;
    double dis1;
    double dis2;
    double dx;
    double dy;
    int wayflags;

    public SnapResult() {

    }

    public void SnapNode() {

    }

    public void virtualNode(int node1, int node2, double dis1, double dis2, double dx, double dy,
            int wayflags) {
        this.node1 = node1;
        this.node2 = node2;
        this.dis1 = dis1;
        this.dis2 = dis2;
        this.dx = dx;
        this.dy = dy;
        this.wayflags = wayflags;

    }

    public int getNode1() {
        return node1;
    }

    public int getNode2() {
        return node2;
    }

    public double getDist1() {
        return dis1;
    }

    public double getDist2() {
        return dis2;
    }
}
