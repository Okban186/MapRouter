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
import com.okban.model.GraphStorage;
import com.okban.uiLayer.MapView;
import com.okban.uiLayer.Abstract.MapFeature;
import com.okban.uiLayer.Implement.BuildingFeature;

import com.okban.uiLayer.Implement.RoadFeature;

import crosby.binary.osmosis.OsmosisReader;
import javafx.concurrent.Task;

public class OsmFileLoadService {

    private GraphStorage graphStorage;

    public OsmFileLoadService() {
        graphStorage = new GraphStorage();
    }

    public Task<OsmDataResult> processPbfFile(File file, MapView mapView) {

        Task<OsmDataResult> loadTask = new Task<>() {

            private double minLon = 106.30;
            private double maxLon = 107.10;
            private double minLat = 10.30;
            private double maxLat = 11.20;

            // private Map<Long, GraphNode> nodeMap = new HashMap<>();
            // private Map<Long, List<MapFeature>> tileIndex = new HashMap<>();
            private HashMap<Long, Pair<Integer, Integer>> idMatching = new HashMap<>();
            // private List<GraphNode> nodeMap = new ArrayList<>();
            // không cấp phát mảng 2D lớn ngay — dùng map sparse tạm thời
            private int tileCellX = (int) Math.max(1, Math.ceil(mapView.worldWidth / mapView.TILE_WIDTH));
            private int tileCellY = (int) Math.max(1, Math.ceil(mapView.worldHeight / mapView.TILE_HEIGHT));
            private Map<Long, List<MapFeature>> sparseTileIndex = new HashMap<>();
            private int maxTileXUsed = -1;
            private int maxTileYUsed = -1;
            private int currentEdgeCount = 0;
            private int edgeSegmentTile = 256;
            private int GRID_SEGMENT_SIZE = (int) (mapView.worldWidth / edgeSegmentTile);
            private int[] tileSegmentOffsets = new int[GRID_SEGMENT_SIZE * GRID_SEGMENT_SIZE];
            private int[] edgeSegmentTileCounts = new int[GRID_SEGMENT_SIZE * GRID_SEGMENT_SIZE];
            private long[] tileSegmentIds;

            @Override
            protected OsmDataResult call() throws Exception {

                try (FileInputStream fips = new FileInputStream(file)) {

                    OsmosisReader reader1 = new OsmosisReader(fips);

                    reader1.setSink(new Sink() {

                        @Override
                        public void process(EntityContainer entityContainer) {

                            try {
                                Entity entity = entityContainer.getEntity();

                                if (entity.getType() == EntityType.Node) {

                                    Node node = (Node) entity;

                                    double xRatio = (node.getLongitude() - minLon) / (maxLon - minLon);
                                    double yRatio = (maxLat - node.getLatitude()) / (maxLat - minLat);

                                    double worldX = xRatio * mapView.worldWidth;
                                    double worldY = yRatio * mapView.worldHeight;

                                    graphStorage.addNode(worldX, worldY, node.getLongitude(), node.getLatitude());

                                    // nodeMap.add(new GraphNode(idCounter, worldX, worldY, node.getLongitude(),
                                    // node.getLatitude()));
                                    idMatching.put(node.getId(), new Pair<>(graphStorage.getNodeCount() - 1, 0));
                                }

                                if (entity.getType() == EntityType.Way) {

                                    Way way = (Way) entity;

                                    // FeatureType type = detectFeatureType(way);

                                    // List<Integer> geometry = new ArrayList<>();
                                    for (WayNode wn : way.getWayNodes()) {

                                        Pair<Integer, Integer> pair = idMatching.get(wn.getNodeId());
                                        if (pair == null) {

                                            continue;
                                        }
                                        pair.value = pair.value + 1;
                                        // geometry.add(pair.getKey());
                                    }

                                    // MapFeature feature = createFeature(type, geometry.stream()
                                    // .mapToInt(i -> i)
                                    // .toArray(), way);

                                    // if (feature != null) {
                                    // insertIntoTiles(feature, mapView);
                                    // }
                                }

                                if (entity.getType() == EntityType.Relation)
                                    return;
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
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

                            try {
                                Entity entity = entityContainer.getEntity();

                                if (entity.getType() == EntityType.Way) {
                                    Way way = (Way) entity;
                                    int size = 0;
                                    if (getTagValue(way, "highway") != null || getTagValue(way, "building") != null) {

                                        if (way.getWayNodes() == null || way.getWayNodes().isEmpty())
                                            return;

                                        String value = getTagValue(way, "oneway");
                                        boolean oneway = value != null && value.equals("yes");

                                        Pair<Integer, Integer> startPair = idMatching == null ? null
                                                : idMatching.get(way.getWayNodes().get(0).getNodeId());
                                        if (startPair == null)
                                            return;

                                        currentEdgeCount = graphStorage.getEdgeCount();

                                        int startIndex = startPair.key;

                                        int currentIndex = 0;

                                        for (int index = 1; index < way.getWayNodes().size(); index++) {
                                            WayNode wn = way.getWayNodes().get(index);
                                            Pair<Integer, Integer> selPair = idMatching == null ? null
                                                    : idMatching.get(wn.getNodeId());
                                            if (selPair == null)
                                                continue;
                                            int selectedIndex = selPair.key;
                                            if (selPair.value >= 2
                                                    || (index == way.getWayNodes().size() - 1
                                                            && startIndex != selectedIndex)) {
                                                double cost = 0;
                                                int prevIndex = startIndex;

                                                int[] shapeNodeIds = new int[index - currentIndex + 1];

                                                int shapeIndex = 0;
                                                shapeNodeIds[shapeIndex++] = startIndex;
                                                for (int i = currentIndex + 1; i <= index; i++) {

                                                    Pair<Integer, Integer> nextPair = idMatching == null ? null
                                                            : idMatching.get(way.getWayNodes().get(i).getNodeId());
                                                    if (nextPair == null)
                                                        continue;
                                                    int nextIndex = nextPair.key;
                                                    if (prevIndex < 0 || nextIndex < 0
                                                            || prevIndex >= graphStorage.getNodeCount()
                                                            || nextIndex >= graphStorage.getNodeCount())
                                                        continue;

                                                    cost += distance(
                                                            graphStorage.getNodeLat(prevIndex),
                                                            graphStorage.getNodeLon(prevIndex),
                                                            graphStorage.getNodeLat(nextIndex),
                                                            graphStorage.getNodeLon(nextIndex));

                                                    shapeNodeIds[shapeIndex++] = nextIndex;

                                                    prevIndex = nextIndex;
                                                }
                                                // Edge edge1 = new Edge(selectedIndex, cost, false, shapeNodeIds,
                                                // edgeGroupIdCounter++);

                                                graphStorage.addEdge(startIndex, selectedIndex, cost, false,
                                                        shapeNodeIds);
                                                // currentStart.getEdges().add(edge1);
                                                if (getTagValue(way, "highway") != null)
                                                    insertIntoEdgeSegmentCount(shapeNodeIds);

                                                size += graphStorage.getShapeLength(graphStorage.getEdgeCount() - 1);

                                                if (!oneway) {
                                                    // Edge edge2 = new Edge(currentStart.getID(), cost, true,
                                                    // shapeNodeIds,
                                                    // edgeGroupIdCounter++);
                                                    graphStorage.addEdge(selectedIndex, startIndex, cost, true, null);
                                                    // selectedNode.getEdges().add(edge2);
                                                }

                                                currentIndex = index;
                                                startIndex = selectedIndex;

                                            }
                                        }
                                        FeatureType featureType = detectFeatureType(way);

                                        MapFeature feature = createFeature(featureType,
                                                graphStorage.getShapeOffset(currentEdgeCount),
                                                size,
                                                way);

                                        if (feature != null) {

                                            insertIntoTiles(feature, mapView);
                                        }

                                    }

                                }

                                if (entity.getType() == EntityType.Relation)
                                    return;
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }

                    });

                    reader2.run();
                    prefixSumOffset();
                    idk();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Chuyển sparseTileIndex thành mảng nhỏ nhất cần thiết để trả về (giữ API cũ)
                int finalX = Math.max(0, maxTileXUsed + 1);
                int finalY = Math.max(0, maxTileYUsed + 1);
                List<MapFeature>[][] finalTileIndex = new ArrayList[finalX][finalY];
                for (Map.Entry<Long, List<MapFeature>> e : sparseTileIndex.entrySet()) {
                    long k = e.getKey();
                    int tx = (int) (k >>> 32);
                    int ty = (int) k;
                    if (tx < finalX && ty < finalY) {
                        finalTileIndex[tx][ty] = e.getValue();

                    }
                }

                graphStorage.compact();
                idMatching = null;
                sparseTileIndex = null;
                tileSegmentOffsets = null;
                edgeSegmentTileCounts = null;

                return new OsmDataResult(graphStorage, finalTileIndex, tileSegmentIds);
            }

            private int getFlatTile(double dx, double dy) {

                int tilex = (int) (dx / edgeSegmentTile);
                int tileY = (int) (dy / edgeSegmentTile);

                tilex = Math.max(0, Math.min(GRID_SEGMENT_SIZE - 1, tilex));
                tileY = Math.max(0, Math.min(GRID_SEGMENT_SIZE - 1, tileY));

                return tilex * GRID_SEGMENT_SIZE + tileY;
            }

            private void prefixSumOffset() {

                int sum = 0;
                for (int i = 0; i < GRID_SEGMENT_SIZE * GRID_SEGMENT_SIZE; i++) {
                    tileSegmentOffsets[i] = sum;
                    sum += edgeSegmentTileCounts[i];
                }

                tileSegmentIds = new long[sum];
            }

            private void insertIntoEdgeSegmentCount(int[] shapeNodeIds) {

                for (int i = 0; i < shapeNodeIds.length - 1; i++) {

                    int n1 = shapeNodeIds[i];
                    int n2 = shapeNodeIds[i + 1];

                    int tile1 = getFlatTile(graphStorage.getNodeX(n1), graphStorage.getNodeY(n1));
                    int tile2 = getFlatTile(graphStorage.getNodeX(n2), graphStorage.getNodeY(n2));

                    edgeSegmentTileCounts[tile1]++;
                    if (tile2 != tile1)
                        edgeSegmentTileCounts[tile2]++;

                }
            }

            private void idk() {
                System.out.println("Loi o day 3");
                int[] shapeNodes = graphStorage.getShapeNodes();
                for (int edgeIndex = 0; edgeIndex < graphStorage.getEdgeCount(); edgeIndex++) {
                    if (graphStorage.isReverse(edgeIndex))
                        continue;
                    int offset = graphStorage.getShapeOffset(edgeIndex);
                    int len = graphStorage.getShapeLen(edgeIndex);
                    for (int i = offset; i < len - 1; i++) {
                        long segmentId = (edgeIndex << 32) | i;

                        int n1 = shapeNodes[offset];
                        int n2 = shapeNodes[offset + 1];

                        System.out.println("Loi o day");

                        int tile1 = getFlatTile(graphStorage.getNodeX(n1), graphStorage.getNodeY(n1));
                        int tile2 = getFlatTile(graphStorage.getNodeX(n2), graphStorage.getNodeY(n2));

                        System.out.println(tile1 + " " + tile2);
                        tileSegmentIds[tileSegmentOffsets[tile1]++] = segmentId;
                        if (tile2 != tile1)
                            tileSegmentIds[tileSegmentOffsets[tile2]++] = segmentId;
                    }
                }

            }

            private MapFeature createFeature(FeatureType type,
                    int segmentOffSet, int segmentLen,
                    Way way) {

                int minLOD = classifyLOD(type, way);

                switch (type) {

                    case ROAD:
                        return new RoadFeature(segmentOffSet, segmentLen, minLOD,
                                getRoadWidth(getTagValue(way, "highway")), 3,
                                way.getTags(), graphStorage);

                    case BUILDING:
                        return new BuildingFeature(segmentOffSet, segmentLen, minLOD, 2, way.getTags(), graphStorage);

                    // case WATER:
                    // return new WaterFeature(geometry, minLOD, estimateWidth(way), 0,
                    // way.getTags(), graphStorage);

                    // case LANDUSE:
                    // return new LanduseFeature(geometry, minLOD, 1, way.getTags(), graphStorage);

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
                maxTileX = Math.min(tileCellX - 1, maxTileX);
                maxTileY = Math.min(tileCellY - 1, maxTileY);

                for (int tx = minTileX; tx <= maxTileX; tx++) {
                    for (int ty = minTileY; ty <= maxTileY; ty++) {
                        long key = (((long) tx) << 32) | (ty & 0xffffffffL);
                        if (sparseTileIndex.get(key) == null) {
                            sparseTileIndex.put(key, new ArrayList<>());
                        }

                        List<MapFeature> list = sparseTileIndex.get(key);
                        if (list == null) {
                            list = new ArrayList<>();
                            sparseTileIndex.put(key, list);
                        }
                        list.add(feature);
                        if (tx > maxTileXUsed)
                            maxTileXUsed = tx;
                        if (ty > maxTileYUsed)
                            maxTileYUsed = ty;
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