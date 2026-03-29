package com.okban.service;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

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
    private List<DijkstraResult> routingPaths;
    private GraphStorage graphStorage;
    private MapView mapView;

    public RoutingService(List<SnapContext> snapContexts, MapView mapView) {
        this.snapContexts = snapContexts;
        this.routingPaths = new ArrayList<>();
        this.mapView = mapView;
    }

    public void setGraphStorage(GraphStorage graphStorage) {
        this.graphStorage = graphStorage;
    }

    public DijkstraResult getRoutingPath(SnapContext start, SnapContext end) {
        return Dijkstra.compute(start, end, graphStorage);
    }

    public void reconstructPath() {
        routingPaths = new ArrayList<>();
        if (snapContexts.size() <= 1)
            return;

        for (int i = 0; i < snapContexts.size(); i++) {
            for (int j = 0; j < snapContexts.size(); j++) {
                if (i == j)
                    continue;
                routingPaths.add(getRoutingPath(snapContexts.get(i), snapContexts.get(j)));
            }
        }

    }

    public RoutingFeature GTS() {
        reconstructPath();
        if (routingPaths == null || routingPaths.size() <= 0)
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

                double cost = getCost(cur, j, n);
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

            int index = a * (n - 1) + (b < a ? b : b - 1);
            DijkstraResult r = routingPaths.get(index);

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

            List<Pair<Integer, Integer>> dPairs = dResult.path;

            for (int index = 0; index < dPairs.size(); index++) {
                int edgeId = dPairs.get(index).getValue();
                int[] shapeNodeIds = graphStorage.getShapeNodeIds(edgeId);

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

    private double getCost(int a, int b, int n) {
        int index = a * (n - 1) + (b < a ? b : b - 1);
        DijkstraResult r = routingPaths.get(index);
        return (r == null) ? Double.MAX_VALUE : r.cost;
    }

    private void twoOptSA(List<Integer> route, int n) {
        double T = 1000; // nhiệt độ ban đầu
        double cooling = 0.995; // tốc độ giảm nhiệt
        double T_min = 1e-3;

        Random rand = new Random();

        while (T > T_min) {
            boolean improved = false;

            for (int i = 1; i < route.size() - 2; i++) {
                for (int j = i + 1; j < route.size() - 1; j++) {

                    double oldCost = 0;
                    for (int k = 0; k < route.size() - 1; k++) {
                        oldCost += getCost(route.get(k), route.get(k + 1), n);
                    }

                    reverse(route, i, j);

                    double newCost = 0;
                    for (int k = 0; k < route.size() - 1; k++) {
                        newCost += getCost(route.get(k), route.get(k + 1), n);
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
