package com.okban.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class GraphStorage {
    private int nodeCount = 0;
    private double xs[] = new double[1024];
    private double ys[] = new double[1024];
    private double lons[] = new double[1024];
    private double lats[] = new double[1024];
    private int head[] = new int[1024];

    private int edgeCount = 0;
    private int[] to = new int[2048];
    private int[] next = new int[2048];
    private double[] cost = new double[2048];
    private boolean[] isReverse = new boolean[2048];
    private int wayflags[] = new int[2024];

    private int[] shapeNodes = new int[4096];
    private int[] shapeOffSets = new int[2048];
    private int[] shapeLengths = new int[2048];

    private int shapeNodeUsed = 0;

    public GraphStorage() {
        Arrays.fill(head, -1);
        Arrays.fill(shapeOffSets, -1);
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public boolean isReverse(int edgeIndex) {
        return isReverse[edgeIndex];
    }

    public int getShapeLength(int edgeIndex) {
        return shapeLengths[edgeIndex];
    }

    public int getEdgeDes(int edgeIndex) {
        return to[edgeIndex];
    }

    public double getNodeX(int nodeId) {
        return xs[nodeId];
    }

    public double getNodeY(int nodeId) {
        return ys[nodeId];
    }

    public double getNodeLon(int nodeId) {
        return lons[nodeId];
    }

    public double getNodeLat(int nodeId) {
        return lats[nodeId];
    }

    public double getEdgeCost(int edgeIndex) {
        return cost[edgeIndex];
    }

    public int[] getShapeNodes() {
        return shapeNodes;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public int getShapeOffset(int edgeIndex) {
        return shapeOffSets[edgeIndex];
    }

    public int getShapeLen(int edgeIndex) {
        return shapeLengths[edgeIndex];
    }

    public void addNode(double x, double y, double lon, double lat) {
        ensureNodeCapacity(nodeCount + 1);
        xs[nodeCount] = x;
        ys[nodeCount] = y;
        lons[nodeCount] = lon;
        lats[nodeCount] = lat;
        head[nodeCount] = -1;
        nodeCount++;

    }

    public void addEdge(int from, int destId, double edgeCost, boolean reverse, int[] shape, int wayflag) {
        ensureEdgeCapacity(edgeCount + 1);

        int off = appendShape(shape);
        int sLen = 0;
        if (reverse) {
            if (edgeCount - 1 < 0)
                return;
            off = shapeOffSets[edgeCount - 1];
            sLen = shapeLengths[edgeCount - 1];
        }
        shapeOffSets[edgeCount] = off;
        shapeLengths[edgeCount] = shape == null ? sLen : shape.length;

        wayflags[edgeCount] = wayflag;

        to[edgeCount] = destId;
        cost[edgeCount] = edgeCost;
        isReverse[edgeCount] = reverse;
        next[edgeCount] = head[from];
        head[from] = edgeCount;
        edgeCount++;

    }

    public int getWayflag(int edgeIndex) {
        return wayflags[edgeIndex];
    }

    public int appendShape(int shape[]) {
        if (shape == null || shape.length == 0)
            return -1;

        ensureShapeCapacity(shapeNodeUsed + shape.length);
        int off = shapeNodeUsed;
        for (int v : shape)
            shapeNodes[shapeNodeUsed++] = v;
        return off;
    }

    public void ensureNodeCapacity(int min) {
        if (min <= xs.length)
            return;
        int newCap = Math.max(min, xs.length * 2);
        xs = Arrays.copyOf(xs, newCap);
        ys = Arrays.copyOf(ys, newCap);
        lons = Arrays.copyOf(lons, newCap);
        lats = Arrays.copyOf(lats, newCap);
        head = Arrays.copyOf(head, newCap);
        Arrays.fill(head, nodeCount, newCap, -1);
    }

    public void ensureEdgeCapacity(int min) {
        if (min <= wayflags.length)
            return;
        int newCap = Math.max(min, wayflags.length * 2);
        to = Arrays.copyOf(to, newCap);
        next = Arrays.copyOf(next, newCap);
        cost = Arrays.copyOf(cost, newCap);
        isReverse = Arrays.copyOf(isReverse, newCap);
        shapeOffSets = Arrays.copyOf(shapeOffSets, newCap);
        shapeLengths = Arrays.copyOf(shapeLengths, newCap);
        wayflags = Arrays.copyOf(wayflags, newCap);
    }

    public void ensureShapeCapacity(int min) {
        if (min <= shapeNodes.length)
            return;
        int newCap = Math.max(min, shapeNodes.length * 2);
        shapeNodes = Arrays.copyOf(shapeNodes, newCap);
    }

    public int[] getShapeNodeIds(int edgeIndex) {
        int len = shapeLengths[edgeIndex];
        if (len <= 0)
            return new int[0];
        int off = shapeOffSets[edgeIndex];
        if (off == -1)
            return null;

        int out[] = new int[len];

        System.arraycopy(shapeNodes, off, out, 0, len);

        return out;

    }

    public void compact() {

        xs = Arrays.copyOf(xs, Math.max(0, nodeCount + 2));
        ys = Arrays.copyOf(ys, Math.max(0, nodeCount + 2));
        lons = Arrays.copyOf(lons, Math.max(0, nodeCount + 2));
        lats = Arrays.copyOf(lats, Math.max(0, nodeCount + 2));
        head = Arrays.copyOf(head, Math.max(0, nodeCount + 2));

        to = Arrays.copyOf(to, Math.max(0, edgeCount + 2));
        next = Arrays.copyOf(next, Math.max(0, edgeCount + 2));
        cost = Arrays.copyOf(cost, Math.max(0, edgeCount + 2));
        isReverse = Arrays.copyOf(isReverse, Math.max(0, edgeCount + 2));
        shapeOffSets = Arrays.copyOf(shapeOffSets, Math.max(0, edgeCount + 2));
        shapeLengths = Arrays.copyOf(shapeLengths, Math.max(0, edgeCount + 2));
        wayflags = Arrays.copyOf(wayflags, edgeCount + 2);

        shapeNodes = Arrays.copyOf(shapeNodes, Math.max(0, shapeNodeUsed + 2));
    }

    public int getHead(int nodeIndex) {
        return head[nodeIndex];
    }

    public Iterable<Integer> edgesFromIterable(int nodeId) {
        final int start = head[nodeId];
        return () -> new Iterator<Integer>() {
            int cur = start;

            @Override
            public boolean hasNext() {
                return cur != -1;
            }

            @Override
            public Integer next() {
                if (cur == -1)
                    throw new java.util.NoSuchElementException();
                int r = cur;
                cur = next[cur];
                return r;
            }
        };
    }
}
