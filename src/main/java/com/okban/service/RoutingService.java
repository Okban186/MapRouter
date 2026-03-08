package com.okban.service;

import java.util.ArrayList;
import java.util.List;

import com.okban.algorithm.Dijkstra;
import com.okban.model.Edge;
import com.okban.model.GraphNode;
import com.okban.uiLayer.MapView;
import com.okban.uiLayer.Abstract.MapFeature;
import com.okban.uiLayer.Implement.RoutingFeature;

import javafx.scene.paint.Color;

public class RoutingService {

    public List<Integer> getRoutingPath(GraphNode startNode, GraphNode endNode, int n, GraphNode[] graphNodes) {
        return Dijkstra.compute(startNode, endNode, n, graphNodes);
    }

    public List<MapFeature>[][] pathToTile(List<Integer> paths, MapView mapView) {
        if (paths == null || paths.size() < 2)
            return null;
        int tileCell = (int) (mapView.worldWidth / mapView.TILE_WIDTH);
        List<MapFeature>[][] tileIndex = new ArrayList[tileCell][tileCell];

        GraphNode[] graphNodes = mapView.getGraphNodes();

        List<GraphNode> geo = new ArrayList<>();

        // thêm node đầu tiên
        geo.add(graphNodes[paths.get(0)]);

        for (int index = 0; index < paths.size() - 1; index++) {

            int currentId = paths.get(index);
            int nextId = paths.get(index + 1);

            GraphNode currentNode = graphNodes[currentId];
            for (Edge e : currentNode.getEdges()) {

                if (e.getDesId() == nextId) {

                    int[] shapeNodeIds = e.getShapeNodeIds();

                    if (shapeNodeIds.length > 0) {

                        if (!e.isReverse()) {
                            for (int i = 0; i < shapeNodeIds.length; i++) {
                                geo.add(graphNodes[shapeNodeIds[i]]);
                            }
                        } else {
                            for (int i = shapeNodeIds.length - 1; i >= 0; i--) {
                                geo.add(graphNodes[shapeNodeIds[i]]);
                            }
                        }

                    }

                    // thêm node đích
                    geo.add(graphNodes[nextId]);
                    break;
                }
            }
        }

        // tạo 1 RoutingFeature duy nhất
        RoutingFeature routeFeature = new RoutingFeature(geo, 0, 0, null, Color.AQUA, 10);

        insertIntoTiles(routeFeature, mapView, tileCell, tileIndex);

        return tileIndex;
    }

    private void insertIntoTiles(MapFeature feature, MapView mapView, int tileCell, List<MapFeature>[][] tileIndex) {

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
