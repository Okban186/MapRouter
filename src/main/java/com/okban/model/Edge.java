package com.okban.model;

public class Edge {
    private int desId;
    private double cost;
    private int shapeNodeIds[];
    private boolean isReverse;
    private int groupId;

    public Edge(int desId, double cost, boolean isReverse, int[] shapeNodeIds, int groupId) {
        this.desId = desId;
        this.cost = cost;
        this.shapeNodeIds = shapeNodeIds;
        this.isReverse = isReverse;
        this.groupId = groupId;
    }

    public int getDesId() {
        return desId;
    }

    public int getGroupId() {
        return groupId;
    }

    public double getCost() {
        return cost;
    }

    public int[] getShapeNodeIds() {
        return shapeNodeIds;
    }

    public boolean isReverse() {
        return isReverse;
    }

}
