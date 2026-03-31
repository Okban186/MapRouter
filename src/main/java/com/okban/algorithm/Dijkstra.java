package com.okban.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import com.okban.Enum.WayFlags;
import com.okban.dto.DijkstraResult;
import com.okban.dto.Pair;
import com.okban.model.GraphStorage;
import com.okban.model.SnapContext;

public class Dijkstra {

  public static DijkstraResult compute(SnapContext start, SnapContext end, GraphStorage graphStorage) {

    PriorityQueue<Pair<Integer, Double>> open = new PriorityQueue<>(Comparator.comparingDouble(Pair::getValue));
    int n = graphStorage.getNodeCount();
    double[] costs = new double[n];
    int[] parent = new int[n];
    int[] parentEdge = new int[n];

    Arrays.fill(parent, -1);
    Arrays.fill(costs, -1);
    BitSet close = new BitSet(n);

    if ((start.getWayflags() & WayFlags.ONEWAY.getValue()) == 0) {
      open.add(new Pair<Integer, Double>(start.getNode1(), 0.0));
      costs[start.getNode1()] = 0;

    }

    open.add(new Pair<Integer, Double>(start.getNode2(), 0.0));

    costs[start.getNode2()] = 0;

    while (!open.isEmpty()) {

      Pair<Integer, Double> current = open.poll();
      int currentNode = current.key;
      double currentCost = current.value;

      if (currentCost > costs[currentNode])
        continue;

      if (close.get(currentNode))
        continue;

      close.set(currentNode, true);

      if (currentNode == end.getNode1()) {
        return buildPath(start, end, end.getNode1(), parent, parentEdge, graphStorage, costs[end.getNode1()]);

      }

      else if (currentNode == end.getNode2() && (end.getWayflags() & WayFlags.ONEWAY.getValue()) == 0) {
        return buildPath(start, end, end.getNode2(), parent, parentEdge, graphStorage, costs[end.getNode2()]);

      }

      for (Integer eIdObj : graphStorage.edgesFromIterable(currentNode)) {
        int eId = eIdObj;
        int wayflag = graphStorage.getWayflag(eId);
        if ((wayflag & WayFlags.FOOTWAY.getValue()) != 0
            || (wayflag & WayFlags.HISTORIC.getValue()) != 0)
          continue;
        int selectedIndex = graphStorage.getEdgeDes(eId);
        if (close.get(selectedIndex))
          continue;

        double newCost = currentCost + graphStorage.getEdgeCost(eId);
        Double oldCost = costs[selectedIndex];

        if (oldCost == -1 || newCost < oldCost) {

          costs[selectedIndex] = newCost;

          parent[selectedIndex] = currentNode;
          parentEdge[selectedIndex] = eId;

          open.add(new Pair<Integer, Double>(selectedIndex, newCost));
        }
      }

    }
    return null;

  }

  private static DijkstraResult buildPath(SnapContext startSnap, SnapContext endSnap, int end, int parent[],
      int parentEdge[],
      GraphStorage graphStorage,
      double cost) {
    List<Pair<Integer, Integer>> path = new ArrayList<>();
    int cur = end;
    while (cur != -1) {

      path.add(new Pair<Integer, Integer>(cur, parentEdge[cur]));
      int next = parent[cur];
      cur = next;
    }

    Collections.reverse(path);

    if (path.get(0).key == startSnap.getNode1()) {
      cost += startSnap.getDist1();
    } else if (path.get(0).key == startSnap.getNode2()) {
      cost += startSnap.getDist2();
    }

    if (path.get(path.size() - 1).key == endSnap.getNode1())
      cost += endSnap.getDist1();
    else if (path.get(path.size() - 1).key == endSnap.getNode2())
      cost += endSnap.getDist2();

    return new DijkstraResult(path, startSnap, endSnap, cost);
  }

}
