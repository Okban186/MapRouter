package com.okban.model;

public class Edge {
    private int desId;
    private double cost;
    private int shapeNodeIds[];
    private boolean isReverse;

    public Edge(int desId, double cost, boolean isReverse, int[] shapeNodeIds) {
        this.desId = desId;
        this.cost = cost;
        this.shapeNodeIds = shapeNodeIds;
        this.isReverse = isReverse;
    }

    public int getDesId() {
        return desId;
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
