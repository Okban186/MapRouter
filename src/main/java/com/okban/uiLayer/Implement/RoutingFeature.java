package com.okban.uiLayer.Implement;

import java.util.Collection;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.model.GraphStorage;

import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class RoutingFeature {

    private Color mainColor;
    private int base;
    private int[] geo;
    protected int minLOD;
    protected BoundingBox boundingBox;
    protected int layer;
    protected Collection<Tag> tags;
    protected int lastDrawFrame = -1;

    public RoutingFeature(int minLOD, Color mainColor, int base, GraphStorage graphStorage, int[] geo) {
        // super(segmentOffSet, segmentLen, minLOD, layer, tags, graphStorage);
        this.mainColor = mainColor;
        this.base = base;
        this.geo = geo;
        this.minLOD = minLOD;
        this.base = base;
        this.boundingBox = computeBoundingBox(graphStorage);
    }

    public int getLastDrawFrame() {
        return lastDrawFrame;
    }

    public void setLastDrawFrame(int lastDrawFrame) {
        this.lastDrawFrame = lastDrawFrame;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public int getMinLOD() {
        return minLOD;
    }

    public void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage) {
        if (geo.length < 2)
            return;
        boolean firstPoint = true;
        double lastX = 0;
        double lastY = 0;

        gc.save();
        gc.beginPath();
        gc.setStroke(mainColor);
        gc.setLineWidth(Math.max(5, Math.min(base * zoom, 60)));
        // int[] shapeNodes = graphStorage.getShapeNodes();
        for (int i = 0; i < geo.length; i++) {

            double screenX = (graphStorage.getNodeX(geo[i]) + 512 - cameraX) * zoom;
            double screenY = (graphStorage.getNodeY(geo[i]) + 512 - cameraY) * zoom;

            double dx = screenX - lastX;
            double dy = screenY - lastY;

            if (zoom < 1.5 && !firstPoint && dx * dx + dy * dy < 12) {
                continue;
            }

            if (firstPoint) {
                gc.moveTo(screenX, screenY);
                firstPoint = false;
            } else {
                gc.lineTo(screenX, screenY);
            }

            lastX = screenX;
            lastY = screenY;
        }

        gc.stroke();
        gc.restore();
    }

    public void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage) {

    }

    private BoundingBox computeBoundingBox(GraphStorage graphStorage) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (int index : geo) {
            minX = Math.min(minX, graphStorage.getNodeX(index));
            minY = Math.min(minY, graphStorage.getNodeY(index));
            maxX = Math.max(maxX, graphStorage.getNodeX(index));
            maxY = Math.max(maxY, graphStorage.getNodeY(index));
        }

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

}
