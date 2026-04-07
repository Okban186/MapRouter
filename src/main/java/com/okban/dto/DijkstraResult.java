package com.okban.dto;

import java.util.List;

import com.okban.model.SnapContext;

public class DijkstraResult {
    // public List<Pair<Integer, Integer>> path;
    public int[] nodeIds;
    public int[] edgeIds;
    public SnapContext start;
    public SnapContext end;
    public double cost;

    public DijkstraResult(int nodeIds[], int edgeIds[], SnapContext start, SnapContext end, double cost) {
        this.nodeIds = nodeIds;
        this.edgeIds = edgeIds;
        this.start = start;
        this.end = end;
        this.cost = cost;
    }
}
