package com.okban.dto;

import java.util.List;

import com.okban.model.SnapContext;

public class DijkstraResult {
    public List<Pair<Integer, Integer>> path;
    public SnapContext start;
    public SnapContext end;
    public double cost;

    public DijkstraResult(List<Pair<Integer, Integer>> path, SnapContext start, SnapContext end, double cost) {
        this.path = path;
        this.start = start;
        this.end = end;
        this.cost = cost;
    }
}
