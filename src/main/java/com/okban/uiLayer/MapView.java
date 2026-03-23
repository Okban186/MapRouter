package com.okban.uiLayer;

import java.util.List;

import com.okban.Enum.WayFlags;
import com.okban.dto.OsmDataResult;

import com.okban.model.GraphStorage;
import com.okban.model.SnapResult;
import com.okban.uiLayer.Abstract.MapFeature;
import com.okban.uiLayer.Implement.RoutingFeature;

import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class MapView {
    private Pane map;
    private Pane overlay;
    private Canvas lineConnect;
    private Canvas lineOverlay;
    private double ROOT_WIDTH;
    private double ROOT_HEIGHT;
    public double TILE_WIDTH = 512;
    public double TILE_HEIGHT = 512;
    public double worldWidth = 100_000;
    public double worldHeight = 100_000;
    private double cameraX = 0;
    private double cameraY = 0;
    private int zoomLevel = 1;
    private double zoom;
    private double scrollAccumulator = 0;
    private double lastMouseX;
    private double lastMouseY;
    private int tileCell;
    private int frameId;
    private boolean routingLine;
    private List<RoutingFeature>[][] routingTiles;
    private static final int MAX_FRAME_ID = 1000000;
    private double BUFFER = 512 * 2;
    private boolean isDragg = false;
    private double minLon = 106.30;
    private double maxLon = 107.10;
    private double minLat = 10.30;
    private double maxLat = 11.20;
    private double dragAccumX = 0;
    private double dragAccumY = 0;

    private OsmDataResult dataWrapper;

    public MapView(double ROOT_WIDTH, double ROOT_HEIGHT) {
        this.ROOT_WIDTH = ROOT_WIDTH;
        this.ROOT_HEIGHT = ROOT_HEIGHT;
        tileCell = (int) (worldWidth / TILE_WIDTH);
        // this.TILE_WIDTH = ROOT_WIDTH;
        // this.TILE_HEIGHT = ROOT_HEIGHT;

        updateZoom();
    }

    public Parent createMapView() {

        map = new Pane();
        map.setPrefSize(ROOT_WIDTH, ROOT_HEIGHT);
        map.setBackground(new Background(new BackgroundFill(Color.LIGHTGREY, null, null)));
        overlay = new Pane();
        double canvasWidth = ROOT_WIDTH + BUFFER;
        double canvasHeight = ROOT_HEIGHT + BUFFER;

        lineConnect = new Canvas(canvasWidth, canvasHeight);
        lineOverlay = new Canvas(canvasWidth, canvasHeight);

        lineConnect.setTranslateX(-BUFFER / 2);
        lineConnect.setTranslateY(-BUFFER / 2);

        lineOverlay.setTranslateX(-BUFFER / 2);
        lineOverlay.setTranslateY(-BUFFER / 2);

        map.getChildren().addAll(lineConnect, lineOverlay, overlay);
        mapEvent();
        return map;
    }

    public Pane getOverlay() {
        return overlay;
    }

    public Canvas getLineConnect() {
        return lineConnect;
    }

    public Canvas getLineOverlay() {
        return lineOverlay;
    }

    public void setRoutingLine(boolean routingLine) {
        this.routingLine = routingLine;
    }

    public boolean getRoutingLine() {
        return routingLine;
    }

    public void setRoutingTiles(List<RoutingFeature>[][] routingTiles) {
        this.routingTiles = routingTiles;
    }

    public void onDataLoaded(OsmDataResult dataWrapper) {
        this.dataWrapper = dataWrapper;

        // for (List<MapFeature> nodes : buckMap.values())
        // nodes.sort(Comparator.comparingInt(MapFeature::getLayer));
    }

    public GraphStorage getGraphStorage() {
        return dataWrapper.graphStorage;
    }

    public List<MapFeature>[][] getBucketMap() {
        return dataWrapper.tileIndexs;
    }

    public void repaint() {
        List<MapFeature>[][] bucketMap = dataWrapper.tileIndexs;
        if (bucketMap == null || zoom <= 0)
            return;

        GraphicsContext gc = lineConnect.getGraphicsContext2D();
        gc.clearRect(0, 0, lineConnect.getWidth(), lineConnect.getHeight());
        GraphicsContext gcOverlay = lineOverlay.getGraphicsContext2D();
        gcOverlay.clearRect(0, 0, lineOverlay.getWidth(), lineOverlay.getHeight());

        double viewWorldWidth = (ROOT_WIDTH + BUFFER) / zoom;
        double viewWorldHeight = (ROOT_HEIGHT + BUFFER) / zoom;

        double minWorldX = cameraX - BUFFER / 2;
        double minWorldY = cameraY - BUFFER / 2;
        double maxWorldX = cameraX + viewWorldWidth + BUFFER / 2;
        double maxWorldY = cameraY + viewWorldHeight + BUFFER / 2;

        int minTileX = (int) (minWorldX / TILE_WIDTH);
        int maxTileX = (int) (maxWorldX / TILE_WIDTH);
        int minTileY = (int) (minWorldY / TILE_HEIGHT);
        int maxTileY = (int) (maxWorldY / TILE_HEIGHT);

        minTileX = Math.max(0, minTileX);
        minTileY = Math.max(0, minTileY);
        maxTileX = Math.min(tileCell - 1, maxTileX);
        maxTileY = Math.min(tileCell - 1, maxTileY);

        int currentLOD = getLODLevel();

        for (int tx = minTileX; tx <= maxTileX; tx++) {
            for (int ty = minTileY; ty <= maxTileY; ty++) {

                List<MapFeature> features = bucketMap[tx][ty];
                if (features == null)
                    continue;

                for (MapFeature feature : features) {

                    if (feature.getMinLOD() <= currentLOD && feature.getLastDrawFrame() != frameId) {
                        feature.setLastDrawFrame(frameId);
                        feature.draw(
                                gc,
                                cameraX,
                                cameraY,
                                zoom, getGraphStorage());

                        feature.drawLabel(gcOverlay, cameraX, cameraY, zoom, getGraphStorage());
                    }
                }

                if (routingLine) {
                    List<RoutingFeature> routes = routingTiles[tx][ty];
                    if (routes == null)
                        continue;
                    for (RoutingFeature route : routes) {
                        if (route.getMinLOD() <= currentLOD && route.getLastDrawFrame() != frameId) {
                            route.setLastDrawFrame(frameId);
                            route.draw(gcOverlay, cameraX, cameraY, zoom, getGraphStorage());
                        }
                    }
                }

            }
        }

        updateFrameId();
    }

    private void mapEvent() {
        map.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            isDragg = false;
            e.consume();

        });

        map.setOnMouseDragged(e -> {
            isDragg = true;
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            lastMouseX = e.getX();
            lastMouseY = e.getY();

            // double nextAccumX = dragAccumX + dx;
            // double nextAccumY = dragAccumY + dy;

            // double tempCameraX = cameraX - nextAccumX / zoom;
            // double tempCameraY = cameraY - nextAccumY / zoom;

            // double maxCameraX = worldWidth - (ROOT_WIDTH + BUFFER) / zoom;
            // double maxCameraY = worldHeight - (ROOT_HEIGHT + BUFFER) / zoom;

            // if (tempCameraX < 0 && dx > 0) {
            // dx = 0;
            // }

            // if (tempCameraY < 0 && dy > 0) {
            // dy = 0;
            // }

            // if (tempCameraX > maxCameraX && dx < 0) {
            // dx = 0;
            // }

            // if (tempCameraY > maxCameraY && dy < 0) {
            // dy = 0;
            // }

            lineConnect.setTranslateX(lineConnect.getTranslateX() + dx);
            lineConnect.setTranslateY(lineConnect.getTranslateY() + dy);

            lineOverlay.setTranslateX(lineOverlay.getTranslateX() + dx);
            lineOverlay.setTranslateY(lineOverlay.getTranslateY() + dy);

            dragAccumX += dx;
            dragAccumY += dy;

            if (Math.abs(dragAccumX) >= TILE_WIDTH ||
                    Math.abs(dragAccumY) >= TILE_HEIGHT) {

                cameraX -= dragAccumX / zoom;
                cameraY -= dragAccumY / zoom;

                clampCamera();

                lineConnect.setTranslateX(-BUFFER / 2);
                lineConnect.setTranslateY(-BUFFER / 2);

                lineOverlay.setTranslateX(-BUFFER / 2);
                lineOverlay.setTranslateY(-BUFFER / 2);

                dragAccumX = 0;
                dragAccumY = 0;

                repaint();

            }

            e.consume();
        });

        map.setOnMouseClicked(e -> {
            if (isDragg)
                return;

            GraphicsContext g = lineOverlay.getGraphicsContext2D();
            Point2D clicked = lineOverlay.sceneToLocal(e.getSceneX(), e.getSceneY());
            double wx = (clicked.getX() / zoom) - 512 + cameraX;
            double wy = (clicked.getY() / zoom) - 512 + cameraY;

            // double xRatio = (node.getLongitude() - minLon) / (maxLon - minLon);
            // double yRatio = (maxLat - node.getLatitude()) / (maxLat - minLat);

            double xRatio = wx / worldWidth;
            double yRatio = wy / worldHeight;

            double wLon = xRatio * (maxLon - minLon) + minLon;
            double wLat = (yRatio * (maxLat - minLat) - maxLat) * -1;

            System.out.println(wLat + " " + wLon);

            g.setFill(Color.RED);
            g.fillOval(clicked.getX(), clicked.getY(), 2, 2);

            double half = 256;
            double minX = wx - half;
            double minY = wy - half;
            double maxX = wx + half;
            double maxY = wy + half;

            // System.out.println(wx / 256 + " " + wy / 256);

            int minTileX = (int) Math.floor(minX / 512);
            int maxTileX = (int) Math.floor(maxX / 512);

            int minTileY = (int) Math.floor(minY / 512);
            int maxTileY = (int) Math.floor(maxY / 512);

            minTileX = Math.max(0, minTileX);
            minTileY = Math.max(0, minTileY);
            maxTileX = Math.min(tileCell - 1, maxTileX);
            maxTileY = Math.min(tileCell - 1, maxTileY);

            System.out.println(minTileX + " -> " + maxTileX + " " + minTileY + " -> " + maxTileY);

            List<MapFeature>[][] tileIndexs = dataWrapper.tileIndexs;
            GraphStorage graphStorage = getGraphStorage();
            int shapeNodes[] = graphStorage.getShapeNodes();
            for (int i = minTileX; i <= maxTileX; i++) {
                for (int j = minTileY; j <= maxTileY; j++) {
                    List<MapFeature> features = tileIndexs[i][j];
                    if (features == null)
                        continue;
                    for (MapFeature feature : features) {

                        if (feature.getMinLOD() >= 3)
                            continue;
                        int offset = feature.getSegmentOffSet();
                        int len = feature.getSegmentLen();
                        double threshold = Math.min(feature.getBase() * zoom, 60);
                        // System.out.println(i + " " + j + " " + offset + " " + len + " " +
                        // shapeNodes.length);

                        int wayflags = feature.getWayflags();
                        for (int index = offset; index < offset + len - 1; index++) {
                            int aIndex = shapeNodes[index];
                            int bIndex = shapeNodes[index + 1];

                            double ax = graphStorage.getNodeX(aIndex);
                            double ay = graphStorage.getNodeY(aIndex);
                            double bx = graphStorage.getNodeX(bIndex);
                            double by = graphStorage.getNodeY(bIndex);

                            if (((Math.abs(bx - wx) > 256 || Math.abs(by - wy) > 256))
                                    && (Math.abs(ax - wx) > 256 || Math.abs(ay - wy) > 256))
                                continue;

                            double abx = bx - ax;
                            double aby = by - ay;

                            double apx = wx - ax;
                            double apy = wy - ay;

                            double ab2 = abx * abx + aby * aby;

                            if (ab2 == 0)
                                continue;

                            double t = (apx * abx + apy * aby) / ab2;

                            if (t < 0 || t > 1)
                                continue;

                            double hx = ax + t * abx;
                            double hy = ay + t * aby;

                            double dx = wx - hx;
                            double dy = wy - hy;

                            double dist2 = dx * dx + dy * dy;

                            if (dist2 * zoom <= threshold) {

                                // System.out.println(dist2 + " " + graphStorage.getNodeLat(aIndex) + " "
                                // + graphStorage.getNodeLon(aIndex) + " " + graphStorage.getNodeLat(bIndex) + "
                                // "
                                // + graphStorage.getNodeLon(bIndex));

                                // double axv = (ax + 512 - cameraX) * zoom;
                                // double ayv = (ay + 512 - cameraY) * zoom;

                                // double bxv = (bx + 512 - cameraX) * zoom;
                                // double byv = (by + 512 - cameraY) * zoom;
                                // g.fillOval(axv, ayv, 4, 4);
                                // g.fillOval(bxv, byv, 4, 4);
                                // g.restore();

                                SnapResult result = new SnapResult();
                                if ((wayflags & WayFlags.ONEWAY.getValue()) != 0) {
                                    double distB = haversineDistance(wLat, wLon, graphStorage.getNodeLat(bIndex),
                                            graphStorage.getNodeLon(bIndex));
                                    result.virtualNode(-1, bIndex, -1, distB, wx, wy, wayflags);
                                    // System.out.println(graphStorage.getNodeLat(aIndex) + " " +
                                    // graphStorage.getNodeLon(aIndex) + " " + graphStorage.getNodeLat(bIndex)
                                    // + " " +
                                    // graphStorage.getNodeLon(bIndex) + " " + -1 + " " + distB);
                                    System.out.println(aIndex + " " + bIndex + " " + -1 + " " + distB);
                                } else {
                                    double distA = haversineDistance(wLat, wLon, graphStorage.getNodeLat(aIndex),
                                            graphStorage.getNodeLon(aIndex));
                                    double distB = haversineDistance(wLat, wLon, graphStorage.getNodeLat(bIndex),
                                            graphStorage.getNodeLon(bIndex));
                                    result.virtualNode(aIndex, bIndex, distA, distB, wx, wy, wayflags);
                                    // System.out.println(graphStorage.getNodeLat(aIndex) + " " +
                                    // graphStorage.getNodeLon(aIndex) + " " + graphStorage.getNodeLat(bIndex)
                                    // + " " +
                                    // graphStorage.getNodeLon(bIndex) + " " + distA + " " + distB);
                                    // }
                                    System.out.println(aIndex + " " + bIndex + " " + distA + " " + distB);

                                }

                                return;
                            }

                        }
                    }
                }
            }

        });
        map.setOnScroll(e -> {

            scrollAccumulator += e.getDeltaY();

            if (scrollAccumulator > 40) {
                zoomLevel++;
                scrollAccumulator = 0;
            } else if (scrollAccumulator < -40) {
                zoomLevel--;
                scrollAccumulator = 0;
            }

            zoomLevel = Math.max(-40, Math.min(zoomLevel, 40));

            double oldZoom = zoom;

            double mouseX = e.getX();
            double mouseY = e.getY();

            // Van de la khi zoom thi cameraX, cameraY dich chuyen nhung canvas van o vi tri
            // cu
            // can tru them dragAccum boi vi khi camera bam theo map nhung canvas van o vi
            // tri cu nen camera van can phan
            // dich them dragAccum tuc la tinh tu vi tri -BUFFER/2 den translate hien tai

            double worldXBefore = cameraX + (mouseX - dragAccumX + BUFFER / 2) / oldZoom - BUFFER / 2;
            double worldYBefore = cameraY + (mouseY - dragAccumY + BUFFER / 2) / oldZoom - BUFFER / 2;

            updateZoom();

            double worldXAfter = cameraX + (mouseX - dragAccumX + BUFFER / 2) / zoom - BUFFER / 2;
            double worldYAfter = cameraY + (mouseY - dragAccumY + BUFFER / 2) / zoom - BUFFER / 2;

            cameraX += worldXBefore - worldXAfter;
            cameraY += worldYBefore - worldYAfter;

            clampCamera();

            repaint();
        });
    }

    private void clampCamera() {

        double viewWorldWidth = (ROOT_WIDTH + BUFFER) / zoom;
        double viewWorldHeight = (ROOT_HEIGHT + BUFFER) / zoom;

        double minCameraX = Math.min(0, worldWidth - viewWorldWidth);
        double maxCameraX = Math.max(0, worldWidth - viewWorldWidth);

        double minCameraY = Math.min(0, worldHeight - viewWorldHeight);
        double maxCameraY = Math.max(0, worldHeight - viewWorldHeight);

        cameraX = Math.max(minCameraX, Math.min(cameraX, maxCameraX));
        cameraY = Math.max(minCameraY, Math.min(cameraY, maxCameraY));
    }

    private void updateZoom() {
        zoom = Math.pow(1.08, zoomLevel);
    }

    private int getLODLevel() {

        if (zoomLevel <= -38)
            return 0;
        if (zoomLevel <= -20)
            return 1;
        if (zoomLevel <= -5)
            return 2;
        return 3;
    }

    public void updateFrameId() {
        frameId = (frameId + 1) % MAX_FRAME_ID;
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

}