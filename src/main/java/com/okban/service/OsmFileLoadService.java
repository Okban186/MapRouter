package com.okban.service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.okban.Enum.FeatureType;
import com.okban.dto.OsmDataResult;
import com.okban.dto.Pair;
import com.okban.model.Edge;
import com.okban.model.GraphNode;
import com.okban.uiLayer.MapView;
import com.okban.uiLayer.Abstract.MapFeature;
import com.okban.uiLayer.Implement.BuildingFeature;
import com.okban.uiLayer.Implement.LanduseFeature;
import com.okban.uiLayer.Implement.RoadFeature;
import com.okban.uiLayer.Implement.WaterFeature;

import crosby.binary.osmosis.OsmosisReader;
import javafx.concurrent.Task;

public class OsmFileLoadService {

    public Task<OsmDataResult> processPbfFile(File file, MapView mapView) {

        Task<OsmDataResult> loadTask = new Task<>() {

            private double minLon = 106.30;
            private double maxLon = 107.10;
            private double minLat = 10.30;
            private double maxLat = 11.20;
            private int idCounter = 0;

            // private Map<Long, GraphNode> nodeMap = new HashMap<>();
            // private Map<Long, List<MapFeature>> tileIndex = new HashMap<>();
            private HashMap<Long, Pair<Integer, Integer>> idMatching = new HashMap<>();
            private List<GraphNode> nodeMap = new ArrayList<>();
            private int tileCell = (int) (mapView.worldWidth / mapView.TILE_WIDTH);
            private List<MapFeature>[][] tileIndex = new ArrayList[tileCell][tileCell];
            private int edgeGroupIdCounter = 0;

            @Override
            protected OsmDataResult call() throws Exception {

                try (FileInputStream fips = new FileInputStream(file)) {

                    OsmosisReader reader1 = new OsmosisReader(fips);

                    reader1.setSink(new Sink() {

                        @Override
                        public void process(EntityContainer entityContainer) {

                            Entity entity = entityContainer.getEntity();

                            if (entity.getType() == EntityType.Node) {

                                Node node = (Node) entity;

                                double xRatio = (node.getLongitude() - minLon) / (maxLon - minLon);
                                double yRatio = (maxLat - node.getLatitude()) / (maxLat - minLat);

                                double worldX = xRatio * mapView.worldWidth;
                                double worldY = yRatio * mapView.worldHeight;

                                nodeMap.add(new GraphNode(idCounter, worldX, worldY, node.getLongitude(),
                                        node.getLatitude()));
                                idMatching.put(node.getId(), new Pair<>(idCounter++, 0));
                            }

                            if (entity.getType() == EntityType.Way) {

                                Way way = (Way) entity;

                                FeatureType type = detectFeatureType(way);

                                List<GraphNode> geometry = new ArrayList<>();
                                for (WayNode wn : way.getWayNodes()) {

                                    Pair<Integer, Integer> pair = idMatching.get(wn.getNodeId());
                                    GraphNode n = nodeMap.get(pair.key);
                                    // if ("10.9037199 106.84495220000001".equals(n.getLat() + " " + n.getLon())) {
                                    // System.out.println(getTagValue(way, "highway"));
                                    // for (WayNode we : way.getWayNodes()) {

                                    // Pair<Integer, Integer> p = idMatching.get(we.getNodeId());
                                    // GraphNode node = nodeMap.get(p.key);
                                    // System.out.print(
                                    // node.getLat() + " " + node.getLon() + " " + node.getID() + " -> ");
                                    // }
                                    // System.out.println();
                                    // }
                                    pair.value = pair.value + 1;
                                    if (n != null) {
                                        geometry.add(n);
                                    }
                                }

                                MapFeature feature = createFeature(type, geometry, way);

                                if (feature != null) {
                                    insertIntoTiles(feature, mapView);
                                }
                            }

                            if (entity.getType() == EntityType.Relation)
                                return;
                        }

                        @Override
                        public void initialize(Map<String, Object> metaData) {
                        }

                        @Override
                        public void complete() {

                        }

                        @Override
                        public void close() {

                        }
                    });

                    reader1.run();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                try (FileInputStream fips = new FileInputStream(file)) {
                    OsmosisReader reader2 = new OsmosisReader(fips);
                    reader2.setSink(new Sink() {

                        @Override
                        public void initialize(Map<String, Object> metaData) {

                        }

                        @Override
                        public void complete() {

                        }

                        @Override
                        public void close() {

                        }

                        @Override
                        public void process(EntityContainer entityContainer) {

                            Entity entity = entityContainer.getEntity();

                            if (entity.getType() == EntityType.Way) {
                                Way way = (Way) entity;
                                if (getTagValue(way, "highway") != null) {

                                    String value = getTagValue(way, "oneway");
                                    boolean oneway = value != null && value.equals("yes");
                                    GraphNode currentStart = nodeMap
                                            .get(idMatching.get(way.getWayNodes().get(0).getNodeId()).key);
                                    int currentIndex = 0;
                                    for (int index = 1; index < way.getWayNodes().size(); index++) {
                                        WayNode wn = way.getWayNodes().get(index);
                                        GraphNode selectedNode = nodeMap.get(idMatching.get(wn.getNodeId()).key);
                                        if (idMatching.get(wn.getNodeId()).value >= 2
                                                || index == way.getWayNodes().size() - 1) {
                                            double cost = 0;
                                            GraphNode prev = currentStart;
                                            int[] shapeNodeIds = new int[index - currentIndex - 1];
                                            int shapeIndex = 0;
                                            for (int i = currentIndex + 1; i <= index; i++) {

                                                GraphNode next = nodeMap
                                                        .get(idMatching.get(way.getWayNodes().get(i).getNodeId()).key);

                                                cost += distance(
                                                        prev.getLat(), prev.getLon(),
                                                        next.getLat(), next.getLon());
                                                if (i != index) {
                                                    shapeNodeIds[shapeIndex++] = next.getID();
                                                }

                                                prev = next;
                                            }
                                            Edge edge1 = new Edge(selectedNode.getID(), cost, false, shapeNodeIds,
                                                    edgeGroupIdCounter++);
                                            currentStart.getEdges().add(edge1);

                                            if (!oneway) {
                                                Edge edge2 = new Edge(currentStart.getID(), cost, true, shapeNodeIds,
                                                        edgeGroupIdCounter++);
                                                selectedNode.getEdges().add(edge2);
                                            }

                                            currentIndex = index;
                                            currentStart = selectedNode;

                                        }
                                    }
                                }

                            }

                            if (entity.getType() == EntityType.Relation)
                                return;
                        }

                    });

                    reader2.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                idMatching = null;

                return new OsmDataResult(nodeMap.toArray(new GraphNode[nodeMap.size()]), tileIndex);
            }

            private MapFeature createFeature(FeatureType type,
                    List<GraphNode> geometry,
                    Way way) {

                int minLOD = classifyLOD(type, way);

                switch (type) {

                    case ROAD:
                        return new RoadFeature(geometry, minLOD, getRoadWidth(getTagValue(way, "highway")), 3,
                                way.getTags());

                    case BUILDING:
                        return new BuildingFeature(geometry, minLOD, 2, way.getTags());

                    case WATER:
                        return new WaterFeature(geometry, minLOD, estimateWidth(way), 0, way.getTags());

                    case LANDUSE:
                        return new LanduseFeature(geometry, minLOD, 1, way.getTags());

                    default:
                        return null;
                }
            }

            private double getRoadWidth(String highway) {
                switch (highway) {
                    case "motorway":
                        return 13;
                    case "trunk":
                        return 13;
                    case "primary":
                        return 11;
                    case "secondary":
                        return 9;
                    case "tertiary":
                        return 8;
                    case "residential":
                        return 7;
                    default:
                        return 3;
                }
            }

            private void insertIntoTiles(MapFeature feature, MapView mapView) {

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
        };

        new Thread(loadTask).start();
        return loadTask;
    }

    private FeatureType detectFeatureType(Way way) {

        for (Tag tag : way.getTags()) {

            if (tag.getKey().equals("highway"))
                return FeatureType.ROAD;

            if (tag.getKey().equals("building"))
                return FeatureType.BUILDING;

            if (tag.getKey().equals("waterway") ||
                    (tag.getKey().equals("natural") && tag.getValue().equals("water")))
                return FeatureType.WATER;

            if (tag.getKey().equals("landuse"))
                return FeatureType.LANDUSE;
        }

        return null;
    }

    private int classifyLOD(FeatureType type, Way way) {

        switch (type) {

            case WATER:
                return 0;

            case ROAD:
                String highway = getTagValue(way, "highway");

                if (highway == null)
                    return 2;

                if (highway.equals("motorway") || highway.equals("trunk"))
                    return 0;

                if (highway.equals("primary") || highway.equals("secondary"))
                    return 1;

                return 2;

            case BUILDING:
                return 3;

            case LANDUSE:
                return 2;

            default:
                return 3;
        }
    }

    private String getTagValue(Way way, String key) {
        for (Tag tag : way.getTags()) {
            if (tag.getKey().equals(key)) {
                return tag.getValue();
            }
        }
        return null;
    }

    private double estimateWidth(Way way) {
        for (Tag tag : way.getTags()) {
            switch (tag.getKey()) {

                case "river":
                    return 4;

                case "canal":
                    return 3;

                case "stream":
                    return 2;

                default:
                    return 1;
            }
        }

        return 1;
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
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

}