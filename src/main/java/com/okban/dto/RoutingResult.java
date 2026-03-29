package com.okban.dto;

import java.util.List;

import com.okban.model.SnapContext;

public class RoutingResult {
    List<Pair<Integer, Integer>> path;
    double cost;
    SnapContext start;
    SnapContext end;
}
