package com.okban.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.okban.Enum.WayFlags;
import com.okban.algorithm.Dijkstra;
import com.okban.dto.DijkstraResult;
import com.okban.dto.Pair;

import com.okban.model.GraphStorage;
import com.okban.model.SnapContext;
import com.okban.uiLayer.MapView;
import com.okban.uiLayer.Implement.RoutingFeature;

import javafx.geometry.BoundingBox;
import javafx.scene.paint.Color;

public class RoutingService {

    private List<SnapContext> snapContexts;
    private DijkstraResult[][] routingPaths;
    private GraphStorage graphStorage;
    private MapView mapView;
    public double worldWidth = 100_000;
    public double worldHeight = 100_000;
    private double minLon = 106.30;
    private double maxLon = 107.10;
    private double minLat = 10.30;
    private double maxLat = 11.20;

    public RoutingService(List<SnapContext> snapContexts, MapView mapView) {
        this.snapContexts = snapContexts;
        this.mapView = mapView;
    }

    public void setGraphStorage(GraphStorage graphStorage) {
        this.graphStorage = graphStorage;
    }

    public List<DijkstraResult> getRoutingPath(SnapContext start, List<SnapContext> endPoints) {
        return Dijkstra.compute(start, endPoints, graphStorage);
    }

    public void reconstructPath() {
        int n = snapContexts.size();
        routingPaths = new DijkstraResult[n][n];

        if (n <= 1)
            return;

        for (int i = 0; i < n; i++) {

            SnapContext snap1 = snapContexts.get(i);

            List<SnapContext> targets = new ArrayList<>();
            int indexMapping[] = new int[n];
            Arrays.fill(indexMapping, -1);
            int index = 0;
            for (int j = 0; j < n; j++) {
                if (i == j)
                    continue;

                SnapContext snap2 = snapContexts.get(j);

                DijkstraResult same = buildSameSegmentPath(snap1, snap2);

                if (same != null) {
                    routingPaths[i][j] = same;
                } else {
                    targets.add(snap2);
                    indexMapping[index++] = j;
                }
            }
            index = 0;

            if (!targets.isEmpty()) {
                List<DijkstraResult> results = getRoutingPath(snap1, targets);

                for (DijkstraResult r : results) {
                    if (r == null)
                        continue;
                    int j = indexMapping[index++];
                    routingPaths[i][j] = r;
                }
            }
        }
    }

    public DijkstraResult buildSameSegmentPath(SnapContext snap1, SnapContext snap2) {
        int[] shapeNodes = graphStorage.getShapeNodes();
        if (snap1.getNode1() != snap2.getNode1() || snap1.getNode2() != snap2.getNode2()) {
            return null;
        }
        if (snap1.getNearest1() == snap2.getNearest1() &&
                snap1.getNearest2() == snap2.getNearest2()) {

            double xRatio1 = snap1.getX() / worldWidth;
            double yRatio1 = snap1.getY() / worldHeight;

            double wLon1 = xRatio1 * (maxLon - minLon) + minLon;
            double wLat1 = (yRatio1 * (maxLat - minLat) - maxLat) * -1;

            double xRatio2 = snap2.getX() / worldWidth;
            double yRatio2 = snap2.getY() / worldHeight;

            double wLon2 = xRatio2 * (maxLon - minLon) + minLon;
            double wLat2 = (yRatio2 * (maxLat - minLat) - maxLat) * -1;

            double cost = haversineDistance(wLat1, wLon1, wLat2, wLon2);

            if ((snap1.getWayflags() & WayFlags.ONEWAY.getValue()) == 0
                    || snap1.getDist1() <= snap2.getDist1()) {

                return new DijkstraResult(null, null, snap1, snap2, cost);
            }

            return null;
        }

        if (snap1.getDist1() < snap2.getDist1()) {

            double cost = snap2.getDist1() - snap1.getDist1();

            int size = snap2.getOffset1() - snap1.getOffset2() + 1;
            int[] nodeIds = new int[size];

            int i = 0;
            for (int index = snap1.getOffset2(); index <= snap2.getOffset1(); index++) {
                nodeIds[i++] = shapeNodes[index];
            }

            return new DijkstraResult(nodeIds, null, snap1, snap2, cost);
        }

        if ((snap1.getWayflags() & WayFlags.ONEWAY.getValue()) == 0) {

            double cost = snap1.getDist1() - snap2.getDist1();

            int size = snap1.getOffset1() - snap2.getOffset2() + 1;
            int[] nodeIds = new int[size];

            int i = 0;
            for (int index = snap1.getOffset1(); index >= snap2.getOffset2(); index--) {
                nodeIds[i++] = shapeNodes[index];
            }

            return new DijkstraResult(nodeIds, null, snap1, snap2, cost);
        }

        return null;
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Công thức Haversine
        final double R = 6371000;

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                        * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public RoutingFeature GTS() {
        reconstructPath();
        if (routingPaths == null || routingPaths.length <= 0)
            return null;
        int n = snapContexts.size();

        BitSet visit = new BitSet(n);
        List<Integer> route = new ArrayList<>();

        int cur = 0;
        route.add(cur);

        for (int i = 0; i < n; i++) {
            if (visit.get(cur))
                continue;

            visit.set(cur);

            int next = cur;
            double minCost = Double.MAX_VALUE;

            for (int j = 0; j < n; j++) {
                if (cur == j || visit.get(j))
                    continue;

                double cost = getCost(cur, j);
                if (cost < minCost) {
                    next = j;
                    minCost = cost;
                }
            }

            if (cur == next)
                break;

            cur = next;
            route.add(cur);
        }

        route.add(0);

        twoOptSA(route, n);

        List<DijkstraResult> paths = new ArrayList<>();
        double total = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            int a = route.get(i);
            int b = route.get(i + 1);

            // int index = a * (n - 1) + (b < a ? b : b - 1);
            // int index = a * n + b;
            DijkstraResult r = routingPaths[a][b];

            if (r != null) {
                paths.add(r);
                total += r.cost;
            }
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (DijkstraResult dResult : paths) {
            SnapContext snap1 = dResult.start;
            SnapContext snap2 = dResult.end;

            minX = Math.min(minX, snap1.getX());
            minY = Math.min(minY, snap1.getY());
            maxX = Math.max(maxX, snap1.getX());
            maxY = Math.max(maxY, snap1.getY());

            minX = Math.min(minX, snap2.getX());
            minY = Math.min(minY, snap2.getY());
            maxX = Math.max(maxX, snap2.getX());
            maxY = Math.max(maxY, snap2.getY());

            int nodeIds[] = dResult.nodeIds;
            int edgeIds[] = dResult.edgeIds;
            if (nodeIds == null && edgeIds == null)
                continue;

            for (int index = 0; index < nodeIds.length; index++) {

                if (edgeIds == null) {
                    double x = graphStorage.getNodeX(nodeIds[index]);
                    double y = graphStorage.getNodeY(nodeIds[index]);

                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                } else {
                    int[] shapeNodeIds = graphStorage.getShapeNodeIds(edgeIds[index]);

                    if (shapeNodeIds.length == 0)
                        continue;

                    for (int nodeId : shapeNodeIds) {
                        double x = graphStorage.getNodeX(nodeId);
                        double y = graphStorage.getNodeY(nodeId);

                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
        }
        BoundingBox boundingBox = new BoundingBox(minX, minY, maxX - minX, maxY - minY);

        int minTileX = (int) (minX / mapView.TILE_WIDTH);
        int maxTileX = (int) (maxX / mapView.TILE_WIDTH);
        int minTileY = (int) (minY / mapView.TILE_HEIGHT);
        int maxTileY = (int) (maxY / mapView.TILE_HEIGHT);

        int tileCell = (int) (mapView.worldWidth / mapView.TILE_WIDTH);

        minTileX = Math.max(0, minTileX);
        minTileY = Math.max(0, minTileY);
        maxTileX = Math.min(tileCell - 1, maxTileX);
        maxTileY = Math.min(tileCell - 1, maxTileY);

        return new RoutingFeature(
                0, Color.BLUE, 3,
                graphStorage,
                paths,
                boundingBox,
                maxTileX, minTileX,
                maxTileY, minTileY,
                total);

    }

    private double getCost(int a, int b) {
        // int index = a * (n - 1) + (b < a ? b : b - 1);
        // int index = a * n + b;
        DijkstraResult r = routingPaths[a][b];
        return (r == null) ? Double.MAX_VALUE : r.cost;
    }

    private void twoOptSA(List<Integer> route, int n) {
        double T = 3000; // nhiệt độ ban đầu
        double cooling = 0.995; // tốc độ giảm nhiệt
        double T_min = 1e-3;

        Random rand = new Random();

        while (T > T_min) {
            boolean improved = false;

            for (int i = 1; i < route.size() - 2; i++) {
                for (int j = i + 1; j < route.size() - 1; j++) {

                    double oldCost = 0;
                    for (int k = 0; k < route.size() - 1; k++) {
                        oldCost += getCost(route.get(k), route.get(k + 1));
                    }

                    reverse(route, i, j);

                    double newCost = 0;
                    for (int k = 0; k < route.size() - 1; k++) {
                        newCost += getCost(route.get(k), route.get(k + 1));
                    }

                    double delta = newCost - oldCost;

                    if (delta < 0 || Math.exp(-delta / T) > rand.nextDouble()) {
                        improved = true;
                    } else {

                        reverse(route, i, j);
                    }
                }
            }

            T *= cooling;

            if (!improved)
                break;
        }
    }

    private void reverse(List<Integer> route, int i, int j) {
        while (i < j) {
            int tmp = route.get(i);
            route.set(i, route.get(j));
            route.set(j, tmp);
            i++;
            j--;
        }
    }
}
