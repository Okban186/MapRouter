package com.okban.uiLayer;

import java.util.List;

import com.okban.dto.OsmDataResult;
import com.okban.model.GraphNode;
import com.okban.uiLayer.Abstract.MapFeature;

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
    private List<MapFeature>[][] routingTiles;
    private static final int MAX_FRAME_ID = 1000000;

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
        lineConnect = new Canvas(ROOT_WIDTH, ROOT_HEIGHT);
        lineOverlay = new Canvas(ROOT_WIDTH, ROOT_HEIGHT);

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

    public void setRoutingTiles(List<MapFeature>[][] routingTiles) {
        this.routingTiles = routingTiles;
    }

    public void onDataLoaded(OsmDataResult dataWrapper) {
        this.dataWrapper = dataWrapper;

        // for (List<MapFeature> nodes : buckMap.values())
        // nodes.sort(Comparator.comparingInt(MapFeature::getLayer));
    }

    public List<MapFeature>[][] getBucketMap() {
        return dataWrapper.tileIndexs;
    }

    public GraphNode[] getGraphNodes() {
        return dataWrapper.graphNodes;
    }

    public void repaint() {
        List<MapFeature>[][] bucketMap = dataWrapper.tileIndexs;
        if (bucketMap == null || zoom <= 0)
            return;

        GraphicsContext gc = lineConnect.getGraphicsContext2D();
        gc.clearRect(0, 0, ROOT_WIDTH, ROOT_HEIGHT);
        GraphicsContext gcOverlay = lineOverlay.getGraphicsContext2D();
        gcOverlay.clearRect(0, 0, ROOT_WIDTH, ROOT_HEIGHT);

        double viewWorldWidth = ROOT_WIDTH / zoom;
        double viewWorldHeight = ROOT_HEIGHT / zoom;

        double minWorldX = cameraX;
        double minWorldY = cameraY;
        double maxWorldX = cameraX + viewWorldWidth;
        double maxWorldY = cameraY + viewWorldHeight;

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

                // long key = keyGeneration(tx, ty);

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
                                zoom);

                        feature.drawLabel(gcOverlay, cameraX, cameraY, zoom);
                    }
                }

                if (routingLine) {
                    List<MapFeature> routes = routingTiles[tx][ty];
                    if (routes == null)
                        continue;
                    for (MapFeature route : routes) {
                        if (route.getMinLOD() <= currentLOD && route.getLastDrawFrame() != frameId) {
                            route.setLastDrawFrame(frameId);
                            route.draw(gcOverlay, cameraX, cameraY, zoom);
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
        });

        map.setOnMouseDragged(e -> {

            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            cameraX -= dx / zoom;
            cameraY -= dy / zoom;

            lastMouseX = e.getX();
            lastMouseY = e.getY();
            clampCamera();
            repaint();
        });
        map.setOnScroll(e -> {

            if (!e.isControlDown())
                return;

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
            updateZoom();

            double mouseX = e.getX();
            double mouseY = e.getY();

            double worldXBefore = cameraX + mouseX / oldZoom;
            double worldYBefore = cameraY + mouseY / oldZoom;

            double worldXAfter = cameraX + mouseX / zoom;
            double worldYAfter = cameraY + mouseY / zoom;

            cameraX += worldXBefore - worldXAfter;
            cameraY += worldYBefore - worldYAfter;

            clampCamera();
            repaint();
        });
    }

    private void clampCamera() {

        double viewWorldWidth = ROOT_WIDTH / zoom;
        double viewWorldHeight = ROOT_HEIGHT / zoom;

        cameraX = Math.max(0, Math.min(cameraX, worldWidth - viewWorldWidth));
        cameraY = Math.max(0, Math.min(cameraY, worldHeight - viewWorldHeight));
    }

    private void updateZoom() {
        zoom = Math.pow(1.08, zoomLevel);
    }

    private long keyGeneration(int k1, int k2) {
        return ((long) k1 << 32) | (k2 & 0xffffffffL);
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

}
