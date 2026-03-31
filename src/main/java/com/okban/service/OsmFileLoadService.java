package com.okban.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.MatchResult;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.okban.Enum.FeatureType;
import com.okban.Enum.HighwayType;
import com.okban.Enum.WayFlags;
import com.okban.dto.OsmDataResult;
import com.okban.dto.Pair;
import com.okban.model.GraphStorage;
import com.okban.model.POI;
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

    public Task<OsmDataResult> processPbfFile(Path path, MapView mapView) {

        Task<OsmDataResult> loadTask = new Task<>() {

            private double minLon = 106.30;
            private double maxLon = 107.10;
            private double minLat = 10.30;
            private double maxLat = 11.20;

            private HashMap<Long, Pair<Integer, Integer>> idMatching = new HashMap<>();

            // không cấp phát mảng 2D lớn ngay — dùng map sparse tạm thời
            private int tileCellX = (int) Math.max(1, Math.ceil(mapView.worldWidth / mapView.TILE_WIDTH));
            private int tileCellY = (int) Math.max(1, Math.ceil(mapView.worldHeight / mapView.TILE_HEIGHT));
            private Map<Long, List<MapFeature>> sparseTileIndex = new HashMap<>();

            private int maxTileXUsed = -1;
            private int maxTileYUsed = -1;
            private int currentEdgeCount = 0;

            @Override
            protected OsmDataResult call() throws Exception {

                try (InputStream fips = Files.newInputStream(path)) {

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
                                    idMatching.put(node.getId(), new Pair<>(graphStorage.getNodeCount() - 1, 0));

                                }

                                if (entity.getType() == EntityType.Way) {

                                    Way way = (Way) entity;
                                    String name = getTagValue(way, "name");
                                    if (name != null
                                            && name.toLowerCase().contains("bến nhà rồng")) {
                                        for (Tag t : way.getTags())
                                            System.out.println(t.getKey() + " " + t.getValue());
                                        System.out.println(way.getWayNodes().size());

                                    }
                                    for (WayNode wn : way.getWayNodes()) {

                                        Pair<Integer, Integer> pair = idMatching.get(wn.getNodeId());
                                        if (pair == null) {

                                            continue;
                                        }
                                        pair.value = pair.value + 1;

                                    }
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

                try (InputStream fips = Files.newInputStream(path)) {
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
                                    int wayflags = wayflagsBuild(way);
                                    if (wayflags != 0) {

                                        if (way.getWayNodes() == null || way.getWayNodes().isEmpty())
                                            return;

                                        boolean oneway = (wayflags & WayFlags.ONEWAY.getValue()) != 0;

                                        Pair<Integer, Integer> startPair = idMatching == null ? null
                                                : idMatching.get(way.getWayNodes().get(0).getNodeId());
                                        if (startPair == null)
                                            return;

                                        currentEdgeCount = graphStorage.getEdgeCount();

                                        int startIndex = startPair.key;

                                        int currentSegmentStart = 0;

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

                                                int[] shapeNodeIds = new int[index - currentSegmentStart + 1];
                                                // tong so node co trong edge de tao thanh topo bao gom endNode dau va
                                                // cuoi

                                                int shapeIndex = 0;
                                                shapeNodeIds[shapeIndex++] = startIndex;
                                                for (int i = currentSegmentStart + 1; i <= index; i++) {

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

                                                graphStorage.addEdge(startIndex, selectedIndex, cost, false,
                                                        shapeNodeIds, wayflags);

                                                size += graphStorage.getShapeLength(graphStorage.getEdgeCount() - 1);

                                                if (!oneway) {

                                                    graphStorage.addEdge(selectedIndex, startIndex, cost, true, null,
                                                            wayflags);

                                                }

                                                currentSegmentStart = index;
                                                startIndex = selectedIndex;

                                            }
                                        }
                                        FeatureType featureType = detectFeatureType(wayflags);

                                        MapFeature feature = createFeature(featureType,
                                                graphStorage.getShapeOffset(currentEdgeCount),
                                                size,
                                                way, wayflags);

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

                } catch (

                Exception e) {
                    e.printStackTrace();
                }

                // Chuyển sparseTileIndex thành mảng nhỏ nhất cần thiết để trả về
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

                return new OsmDataResult(graphStorage, finalTileIndex);
            }

            // private int getFlatTile(double dx, double dy) {

            // int tilex = (int) (dx / edgeSegmentTile);
            // int tileY = (int) (dy / edgeSegmentTile);

            // tilex = Math.max(0, Math.min(GRID_SEGMENT_SIZE - 1, tilex));
            // tileY = Math.max(0, Math.min(GRID_SEGMENT_SIZE - 1, tileY));

            // return tilex * GRID_SEGMENT_SIZE + tileY;
            // }

            private MapFeature createFeature(FeatureType type,
                    int segmentOffSet, int segmentLen,
                    Way way, int wayflags) {
                if (type == null)
                    return null;
                int minLOD = classifyLOD(type, way, wayflags);
                String name = getTagValue(way, "name");
                if (name == "" || name == null)
                    name = getTagValue(way, "name:vi");
                if (name == "" || name == null)
                    name = getTagValue(way, "alt_name");
                if (name == "" || name == null)
                    name = getTagValue(way, "name:en");
                switch (type) {

                    case ROAD:
                        return new RoadFeature(segmentOffSet, segmentLen, minLOD,
                                getHighwayType(getTagValue(way, "highway")), 3,
                                wayflags, name, graphStorage);

                    case BUILDING:
                        return new BuildingFeature(segmentOffSet, segmentLen, minLOD, 2, 1, wayflags,
                                name,
                                graphStorage);

                    default:
                        return null;
                }
            }

            private HighwayType getHighwayType(String highway) {
                switch (highway) {
                    case "motorway":
                        return HighwayType.MOTORWAY;
                    case "trunk":
                        return HighwayType.TRUNK;
                    case "primary":
                        return HighwayType.PRIMARY;
                    case "secondary":
                        return HighwayType.SECONDARY;
                    case "tertiary":
                        return HighwayType.TERTIARY;
                    case "residential":
                        return HighwayType.RESIDENTIAL;
                    case "footway":
                        return HighwayType.FOOTWAY;
                    default:
                        return HighwayType.UNSET;
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

    private FeatureType detectFeatureType(int wayFlags) {

        if ((wayFlags & WayFlags.HIGHWAY.getValue()) != 0) {

            return FeatureType.ROAD;
        }

        if ((wayFlags & WayFlags.BUILDING.getValue()) != 0 || (wayFlags & WayFlags.HISTORIC.getValue()) != 0
                || (wayFlags & WayFlags.TOURISM.getValue()) != 0)
            return FeatureType.BUILDING;

        return null;
    }

    private int classifyLOD(FeatureType type, Way way, int wayflag) {
        if (type == null)
            return 3;
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

                if ((wayflag & WayFlags.HISTORIC.getValue()) != 0 || (wayflag & WayFlags.TOURISM.getValue()) != 0)
                    return 0;
                return 3;

            case LANDUSE:
                return 2;

            default:
                return 3;
        }
    }

    private String getTagValue(Entity e, String key) {
        if (e.getTags() == null)
            return null;
        for (Tag tag : e.getTags()) {
            if (tag.getKey().equals(key)) {
                return tag.getValue();
            }
        }
        return null;
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

    public int wayflagsBuild(Way way) {
        int wayflags = 0;
        for (Tag tag : way.getTags()) {
            // if (tag.getKey().contains("maxspeed"))
            // System.out.println(way.getId() + " " + tag.getKey() + " " + tag.getValue());
            switch (tag.getKey()) {

                case "highway":
                    wayflags |= WayFlags.HIGHWAY.getValue();
                    if (tag.getValue().equals("footway") || tag.getValue().equals("steps"))
                        wayflags |= WayFlags.FOOTWAY.getValue();
                    break;
                case "oneway":
                    if (tag.getValue().equals("yes"))
                        wayflags |= WayFlags.ONEWAY.getValue();
                    break;
                case "building":

                    wayflags |= WayFlags.BUILDING.getValue();
                    break;

                case "historic":

                    wayflags |= WayFlags.HISTORIC.getValue();
                    break;

                case "tourism":
                    wayflags |= WayFlags.TOURISM.getValue();
                    break;
            }
        }

        return wayflags;
    }

}