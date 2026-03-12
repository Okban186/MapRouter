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

import com.okban.dto.Pair;
import com.okban.model.Edge;
import com.okban.model.GraphNode;
import com.okban.model.GraphStorage;

public class Dijkstra {

  public static List<Pair<Integer, Integer>> compute(int startIndex, int endIndex, GraphStorage graphStorage) {
    PriorityQueue<Pair<Integer, Double>> open = new PriorityQueue<>(Comparator.comparingDouble(Pair::getValue));
    int n = graphStorage.getNodeCount();
    double[] costs = new double[n];
    Map<Integer, List<Integer>> edgeCaches = new HashMap<>();
    int[] parent = new int[n];
    int[] parentEdge = new int[n];
    Arrays.fill(parent, -1);
    Arrays.fill(costs, -1);
    BitSet close = new BitSet(n);

    open.add(new Pair<Integer, Double>(startIndex, 0.0));
    costs[startIndex] = 0.0;

    while (!open.isEmpty()) {

      Pair<Integer, Double> current = open.poll();
      int currentIndex = current.key;
      double currentCost = current.value;

      if (close.get(currentIndex))
        continue;

      close.set(currentIndex, true);

      if (currentIndex == endIndex) {
        return buildPath(parent, parentEdge, startIndex, endIndex,
            costs[endIndex], graphStorage);
      }

      for (Integer eIdObj : graphStorage.edgesFromIterable(currentIndex)) {
        int eId = eIdObj;
        int selectedIndex = graphStorage.getEdgeDes(eId);
        if (close.get(selectedIndex))
          continue;

        double newCost = currentCost + graphStorage.getEdgeCost(eId);
        Double oldCost = costs[selectedIndex];

        if (oldCost == -1 || newCost < oldCost) {

          costs[selectedIndex] = newCost;

          parent[selectedIndex] = currentIndex;
          parentEdge[selectedIndex] = eId;

          open.add(new Pair<Integer, Double>(selectedIndex, newCost));
        }
      }

    }
    return null;

  }

  private static List<Pair<Integer, Integer>> buildPath(int[] parent, int parentEdge[],
      int start,
      int end, double cost, GraphStorage graphStorage) {

    List<Pair<Integer, Integer>> path = new ArrayList<>();
    int cur = end;
    while (cur != -1) {
      path.add(new Pair<Integer, Integer>(cur, parentEdge[cur]));
      int next = parent[cur];
      cur = next;
    }

    Collections.reverse(path);

    if (!path.get(0).getKey().equals(start))
      return null;

    // for (Edge e : graphNodes[1064960].getEdges()) {
    // if (true)
    // break;
    // GraphNode node = graphNodes[e.getDesId()];
    // System.out.print(node.getLat() + " " + node.getLon() + " " + node.getID() + "
    // -> ");
    // int[] shapeNodeIds = e.getShapeNodeIds();
    // if (!e.isReverse()) {
    // for (int index = 0; index < shapeNodeIds.length; index++) {
    // GraphNode nodel = graphNodes[shapeNodeIds[index]];
    // System.out.print(nodel.getLat() + " " + nodel.getLon() + " -> ");

    // }
    // } else {
    // for (int index = shapeNodeIds.length - 1; index >= 0; index--) {
    // GraphNode nodel = graphNodes[shapeNodeIds[index]];
    // System.out.print(nodel.getLat() + " " + nodel.getLon() + " -> ");
    // }

    // }
    // System.out.println(e.getCost() + " " + e.isReverse());

    // }

    // StringBuilder sb = new StringBuilder();
    // for (int i = 0; i < path.size() - 1; i++) {
    // GraphNode graphNode = graphNodes[path.get(i)];
    // sb.append(graphNode.getLon() + " " + graphNode.getLat());
    // sb.append(" -> ");
    // for (Edge e : graphNode.getEdges()) {
    // if (e.getDesId() == path.get(i + 1)) {
    // int[] shapeNodeIds = e.getShapeNodeIds();
    // if (!e.isReverse()) {
    // for (int index = 0; index < shapeNodeIds.length; index++) {
    // GraphNode node = graphNodes[shapeNodeIds[index]];
    // sb.append(node.getLon() + " " + node.getLat());
    // sb.append(" -> ");
    // }
    // } else {
    // for (int index = shapeNodeIds.length - 1; index >= 0; index--) {
    // GraphNode node = graphNodes[shapeNodeIds[index]];
    // sb.append(node.getLon() + " " + node.getLat());
    // sb.append(" -> ");
    // }
    // }
    // break;
    // }
    // }
    // 10.903842200000001 106.84528 -> 10.9037199 106.84495220000001
    // 10.9540468 106.9491165
    // }
    // for (int i : path) {
    // if (true)
    // break;
    // GraphNode graphNode = graphNodes[i];
    // if ("10.903842200000001 106.84528".equals(graphNode.getLat() + " " +
    // graphNode.getLon())) {
    // System.out.println(graphNode.getID());

    // for (Edge e : graphNode.getEdges()) {
    // GraphNode node = graphNodes[e.getDesId()];
    // System.out.print(node.getLat() + " " + node.getLon() + " -> ");
    // int[] shapeNodeIds = e.getShapeNodeIds();
    // if (!e.isReverse()) {
    // for (int index = 0; index < shapeNodeIds.length; index++) {
    // GraphNode nodel = graphNodes[shapeNodeIds[index]];
    // System.out.print(nodel.getLat() + " " + nodel.getLon() + " -> ");

    // }
    // } else {
    // for (int index = shapeNodeIds.length - 1; index >= 0; index--) {
    // GraphNode nodel = graphNodes[shapeNodeIds[index]];
    // System.out.print(nodel.getLat() + " " + nodel.getLon() + " -> ");
    // }

    // }
    // System.out.println(e.getCost());
    // }
    // }
    // }

    return path;
  }
}
