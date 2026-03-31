package com.okban.model;

public class SnapContext {

    int node1;
    int node2;
    double dis1;
    double dis2;
    double x;
    double y;
    int wayflags;
    int nearest1;
    int nearest2;
    int offset1;
    int offset2;
    int len1;
    int len2;

    public SnapContext() {

    }

    public void virtualNode(int node1, int node2, double dis1, double dis2, double x, double y,
            int wayflags, int nearest1, int nearest2, int offset1, int offset2, int len1, int len2) {
        this.node1 = node1;
        this.node2 = node2;
        this.dis1 = dis1;
        this.dis2 = dis2;
        this.x = x;
        this.y = y;
        this.wayflags = wayflags;
        this.nearest1 = nearest1;
        this.nearest2 = nearest2;
        this.offset1 = offset1;
        this.offset2 = offset2;
        this.len1 = len1;
        this.len2 = len2;

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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getWayflags() {
        return wayflags;
    }

    public int getNearest1() {
        return nearest1;
    }

    public int getNearest2() {
        return nearest2;
    }

    public int getOffset1() {
        return offset1;
    }

    public int getOffset2() {
        return offset2;
    }

    public int getLen1() {
        return len1;
    }

    public int getLen2() {
        return len2;
    }
}
