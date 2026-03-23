package com.okban.service;

import java.util.ArrayList;
import java.util.List;

import com.okban.algorithm.Dijkstra;
import com.okban.dto.Pair;

import com.okban.model.GraphStorage;
import com.okban.model.SnapResult;
import com.okban.uiLayer.MapView;
import com.okban.uiLayer.Abstract.MapFeature;
import com.okban.uiLayer.Implement.RoutingFeature;

import javafx.scene.paint.Color;

public class RoutingService {

    public List<Pair<Integer, Integer>> getRoutingPath(SnapResult start, SnapResult end, GraphStorage graphStorage) {
        return Dijkstra.compute(start, end, graphStorage);
    }

    public List<RoutingFeature>[][] pathToTile(List<Pair<Integer, Integer>> paths, MapView mapView) {
        if (paths == null || paths.size() < 2)
            return null;
        int tileCell = (int) (mapView.worldWidth / mapView.TILE_WIDTH);
        List<RoutingFeature>[][] tileIndex = new ArrayList[tileCell][tileCell];

        GraphStorage graphStorage = mapView.getGraphStorage();

        List<Integer> geo = new ArrayList<>();

        // thêm node đầu tiên
        geo.add(paths.get(0).getKey());

        for (int index = 0; index < paths.size() - 1; index++) {

            // int currentId = paths.get(index).getKey();
            int nextId = paths.get(index + 1).getKey();
            int nextGroupId = paths.get(index + 1).getValue();

            int[] shapeNodeIds = graphStorage.getShapeNodeIds(nextGroupId);

            if (shapeNodeIds.length > 0) {

                if (!graphStorage.isReverse(nextGroupId)) {
                    for (int i = 0; i < shapeNodeIds.length; i++) {
                        geo.add(shapeNodeIds[i]);
                    }
                } else {
                    for (int i = shapeNodeIds.length - 1; i >= 0; i--) {
                        geo.add(shapeNodeIds[i]);
                    }
                }

            }

            geo.add(nextId);

        }

        RoutingFeature routeFeature = new RoutingFeature(0,
                Color.AQUA,
                10, mapView.getGraphStorage(), geo.stream().mapToInt(i -> i).toArray());

        insertIntoTiles(routeFeature, mapView, tileCell, tileIndex);

        return tileIndex;
    }

    private void insertIntoTiles(RoutingFeature feature, MapView mapView, int tileCell,
            List<RoutingFeature>[][] tileIndex) {

        var box = feature.getBoundingBox();

        int minTileX = (int) (box.getMinX() / mapView.TILE_WIDTH);
        int maxTileX = (int) (box.getMaxX() / mapView.TILE_WIDTH);
        int minTileY = (int) (box.getMinY() / mapView.TILE_HEIGHT);
        int maxTileY = (int) (box.getMaxY() / mapView.TILE_HEIGHT);
        minTileX = Math.max(0, minTileX);
        minTileY = Math.max(0, minTileY);
        maxTileX = Math.min(tileCell - 1, maxTileX);
        maxTileY = Math.min(tileCell - 1, maxTileY);

        for (int tx = minTileX; tx <= maxTileX; tx++) {
            for (int ty = minTileY; ty <= maxTileY; ty++) {

                if (tileIndex[tx][ty] == null) {
                    tileIndex[tx][ty] = new ArrayList<>();
                }

                tileIndex[tx][ty].add(feature);
            }
        }
    }
}
