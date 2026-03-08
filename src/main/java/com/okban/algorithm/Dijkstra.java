package com.okban.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import com.okban.dto.Pair;
import com.okban.model.Edge;
import com.okban.model.GraphNode;

public class Dijkstra {

    public static List<Integer> compute(GraphNode startNode, GraphNode endNode, int n, GraphNode[] graphNodes) {

        PriorityQueue<Pair<Integer, Double>> open = new PriorityQueue<>(Comparator.comparingDouble(Pair::getValue));

        double[] costs = new double[n];
        int[] parent = new int[n];
        Arrays.fill(parent, -1);

        boolean[] close = new boolean[n];

        open.add(new Pair<Integer, Double>(startNode.getID(), 0.0));
        costs[(int) startNode.getID()] = 0.0;

        while (!open.isEmpty()) {

            Pair<Integer, Double> current = open.poll();
            GraphNode currentNode = graphNodes[current.key];
            double currentCost = current.value;

            if (close[(int) currentNode.getID()])
                continue;

            close[(int) currentNode.getID()] = true;

            if (currentNode.getID() == endNode.getID()) {
                return buildPath(parent, startNode.getID(), endNode.getID(),
                        costs[endNode.getID()], graphNodes);
            }

            for (Edge e : currentNode.getEdges()) {

                if (close[e.getDesId()])
                    continue;

                double newCost = currentCost + e.getCost();
                Double oldCost = costs[e.getDesId()];

                if (oldCost == 0 || newCost < oldCost) {

                    costs[e.getDesId()] = newCost;

                    parent[e.getDesId()] = currentNode.getID();

                    open.add(new Pair<Integer, Double>(e.getDesId(), newCost));
                }
            }

        }
        return null;

    }

    private static List<Integer> buildPath(int[] parent,
            int start,
            int end, double cost, GraphNode[] graphNodes) {

        List<Integer> path = new ArrayList<>();
        int cur = end;
        while (cur != -1) {
            path.add(cur);
            int next = parent[cur];
            cur = next;
        }

        Collections.reverse(path);

        if (!path.get(0).equals(start))
            return null;

        StringBuilder sb = new StringBuilder();
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
        // }
        for (int i : path) {
            GraphNode graphNode = graphNodes[i];
            if ("10.9540468 106.9491165".equals(graphNode.getLat() + " " + graphNode.getLon())) {
                for (Edge e : graphNode.getEdges()) {
                    GraphNode node = graphNodes[e.getDesId()];
                    System.out.print(node.getLat() + " " + node.getLon() + " -> ");
                    int[] shapeNodeIds = e.getShapeNodeIds();
                    if (!e.isReverse()) {
                        for (int index = 0; index < shapeNodeIds.length; index++) {
                            GraphNode nodel = graphNodes[shapeNodeIds[index]];
                            System.out.print(nodel.getLon() + " " + nodel.getLat() + " -> ");

                        }
                    } else {
                        for (int index = shapeNodeIds.length - 1; index >= 0; index--) {
                            GraphNode nodel = graphNodes[shapeNodeIds[index]];
                            System.out.print(nodel.getLon() + " " + nodel.getLat() + " -> ");
                        }

                    }
                    System.out.println(e.getCost());
                }
            }
        }

        return path;
    }
}
