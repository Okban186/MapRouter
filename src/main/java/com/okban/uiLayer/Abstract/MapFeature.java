package com.okban.uiLayer.Abstract;

import java.util.Collection;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.Enum.WayFlags;
import com.okban.model.GraphStorage;

import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;

public abstract class MapFeature {

    protected int minLOD;

    protected BoundingBox boundingBox;
    protected int layer;

    protected int wayflags;
    protected int lastDrawFrame = -1;
    protected int segmentOffset;
    protected int segmentLen;
    protected double base;
    protected String name;

    public MapFeature(int segmentOffset, int segmentLen, int minLOD, int layer, double base, int wayflags, String name,
            GraphStorage graphStorage) {
        this.segmentOffset = segmentOffset;
        this.segmentLen = segmentLen;
        this.minLOD = minLOD;
        this.boundingBox = computeBoundingBox(graphStorage);
        this.layer = layer;
        this.base = base;
        this.wayflags = wayflags;
        this.name = name;
    }

    public double getBase() {
        return base;
    }

    public int getWayflags() {
        return wayflags;
    }

    public int getLayer() {
        return layer;
    }

    public int getSegmentOffSet() {
        return segmentOffset;
    }

    public int getSegmentLen() {
        return segmentLen;
    }

    public int getMinLOD() {
        return minLOD;
    }

    public int getLastDrawFrame() {
        return lastDrawFrame;
    }

    public void setLastDrawFrame(int frameId) {
        lastDrawFrame = frameId;
    }

    private BoundingBox computeBoundingBox(GraphStorage graphStorage) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        int shapeNodes[] = graphStorage.getShapeNodes();
        for (int index = segmentOffset; index < segmentLen + segmentOffset; index++) {
            minX = Math.min(minX, graphStorage.getNodeX(shapeNodes[index]));
            minY = Math.min(minY, graphStorage.getNodeY(shapeNodes[index]));
            maxX = Math.max(maxX, graphStorage.getNodeX(shapeNodes[index]));
            maxY = Math.max(maxY, graphStorage.getNodeY(shapeNodes[index]));
        }

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public abstract void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom,
            GraphStorage graphStorage);

    public abstract void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom,
            GraphStorage graphStorage);

}