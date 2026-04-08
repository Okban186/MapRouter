package com.okban.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.okban.Enum.VehicleType;
import com.okban.Enum.WayFlags;
import com.okban.config.MapConfig;
import com.okban.dto.DijkstraResult;
import com.okban.dto.Pair;
import com.okban.model.GraphStorage;
import com.okban.model.SnapContext;

import javafx.print.Printer.MarginType;

public class Dijkstra {

  public static List<DijkstraResult> compute(
      SnapContext start,
      List<SnapContext> endPoints,
      GraphStorage graphStorage) {

    int n = graphStorage.getNodeCount();

    // results có sẵn size
    List<DijkstraResult> results = new ArrayList<>(Collections.nCopies(endPoints.size(), null));

    PriorityQueue<Pair<Integer, Double>> open = new PriorityQueue<>(Comparator.comparingDouble(Pair::getValue));

    double[] costs = new double[n];
    int[] parent = new int[n];
    int[] parentEdge = new int[n];
    BitSet close = new BitSet(n);

    Arrays.fill(costs, Double.POSITIVE_INFINITY);
    Arrays.fill(parent, -1);

    if ((start.getWayflags() & WayFlags.ONEWAY.getValue()) == 0) {
      open.add(new Pair<>(start.getNode1(), 0.0));
      costs[start.getNode1()] = 0;
    }

    open.add(new Pair<>(start.getNode2(), 0.0));
    costs[start.getNode2()] = 0;

    Map<Integer, List<Integer>> targetMap = new HashMap<>();
    int totalTargets = 0;

    // can lam vay boi 1 diem snap thi co 2 dau cua 1 segment nen co kha nang no bi
    // trung nhau
    for (int i = 0; i < endPoints.size(); i++) {
      SnapContext end = endPoints.get(i);

      if (end == start)
        continue;
      totalTargets++;
      targetMap.computeIfAbsent(end.getNode1(), k -> new ArrayList<>()).add(i);

      if ((end.getWayflags() & WayFlags.ONEWAY.getValue()) == 0) {
        targetMap.computeIfAbsent(end.getNode2(), k -> new ArrayList<>()).add(i);
      }
    }

    int foundCount = 0;

    while (!open.isEmpty()) {

      if (foundCount == totalTargets)
        break;

      Pair<Integer, Double> current = open.poll();
      int currentNode = current.key;
      double currentCost = current.value;

      if (currentCost > costs[currentNode])
        continue;
      if (close.get(currentNode))
        continue;

      List<Integer> targetIndexes = targetMap.get(currentNode);

      if (targetIndexes != null) {
        for (int idx : targetIndexes) {

          // tránh double count boi vi chung ta su dung dijkstra de quyet toan bo dich de
          // tranh mot endpoints duoc gap lai nhieu lan
          if (results.get(idx) != null)
            continue;

          SnapContext end = endPoints.get(idx);

          results.set(idx,
              buildPath(start, end, currentNode,
                  parent, parentEdge,
                  graphStorage, costs[currentNode]));

          foundCount++;
        }
      }

      close.set(currentNode, true);

      for (Integer eIdObj : graphStorage.edgesFromIterable(currentNode)) {

        int eId = eIdObj;
        int wayflag = graphStorage.getWayflag(eId);

        if (shouldSkip(wayflag, MapConfig.currentVehicleType))
          continue;

        int nextNode = graphStorage.getEdgeDes(eId);

        if (close.get(nextNode))
          continue;

        double newCost = currentCost + graphStorage.getEdgeCost(eId);

        if (newCost < costs[nextNode]) {

          costs[nextNode] = newCost;
          parent[nextNode] = currentNode;
          parentEdge[nextNode] = eId;

          open.add(new Pair<>(nextNode, newCost));
        }
      }
    }

    return results;
  }

  private static DijkstraResult buildPath(SnapContext startSnap, SnapContext endSnap, int end, int parent[],
      int parentEdge[],
      GraphStorage graphStorage,
      double cost) {
    List<Integer> listNodeId = new ArrayList<>();
    List<Integer> listEdgeId = new ArrayList<>();
    int cur = end;

    while (cur != -1) {
      listNodeId.add(cur);
      listEdgeId.add(parentEdge[cur]);
      int next = parent[cur];
      cur = next;

    }

    Collections.reverse(listNodeId);
    Collections.reverse(listEdgeId);

    if (listNodeId.get(0) == startSnap.getNode1()) {
      cost += startSnap.getDist1();
    } else if (listNodeId.get(0) == startSnap.getNode2()) {
      cost += startSnap.getDist2();
    }

    if (listNodeId.get(listNodeId.size() - 1) == endSnap.getNode1())
      cost += endSnap.getDist1();
    else if (listNodeId.get(listNodeId.size() - 1) == endSnap.getNode2())
      cost += endSnap.getDist2();

    int nodeIds[] = new int[listNodeId.size()];
    int edgeIds[] = new int[listEdgeId.size()];
    for (int i = 0; i < listNodeId.size(); i++) {
      nodeIds[i] = listNodeId.get(i);
      edgeIds[i] = listEdgeId.get(i);
    }
    return new DijkstraResult(nodeIds, edgeIds, startSnap, endSnap, cost);
  }

  private static boolean shouldSkip(int wayflags, VehicleType vehicle) {
    switch (vehicle) {
      case CAR:
        return (wayflags & WayFlags.BUILDING.getValue()) != 0
            || (wayflags & WayFlags.FOOTWAY.getValue()) != 0
            || (wayflags & WayFlags.ACCESS_NO.getValue()) != 0
            || (wayflags & WayFlags.VEHICLE_NO.getValue()) != 0
            || (wayflags & WayFlags.PRIVATE.getValue()) != 0
            || (wayflags & WayFlags.MOTORCAR_NO.getValue()) != 0
            || (wayflags & WayFlags.FEE.getValue()) != 0; // ví dụ không đi đường thu phí
      case MOTORCYCLE:
        return (wayflags & WayFlags.BUILDING.getValue()) != 0
            || (wayflags & WayFlags.FOOTWAY.getValue()) != 0
            || (wayflags & WayFlags.ACCESS_NO.getValue()) != 0
            || (wayflags & WayFlags.PRIVATE.getValue()) != 0
            || (wayflags & WayFlags.MOTORCYCLE_NO.getValue()) != 0;
      case PSV: // PSV
        return (wayflags & WayFlags.BUILDING.getValue()) != 0
            || (wayflags & WayFlags.FOOTWAY.getValue()) != 0
            || (wayflags & WayFlags.ACCESS_NO.getValue()) != 0
            || (wayflags & WayFlags.VEHICLE_NO.getValue()) != 0
            || (wayflags & WayFlags.PRIVATE.getValue()) != 0
            || (wayflags & WayFlags.PSV_NO.getValue()) != 0
            || (wayflags & WayFlags.FEE.getValue()) != 0;
      default:
        return true; // an toàn
    }
  }
}
