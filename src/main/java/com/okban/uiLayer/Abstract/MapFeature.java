package com.okban.uiLayer.Abstract;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.model.GraphNode;

import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;

public abstract class MapFeature {

    protected int minLOD;
    protected List<GraphNode> geometry;
    protected BoundingBox boundingBox;
    protected int layer;
    protected Collection<Tag> tags;
    protected int lastDrawFrame = -1;

    public MapFeature(List<GraphNode> geometry, int minLOD, int layer, Collection<Tag> tags) {
        this.geometry = geometry;
        this.minLOD = minLOD;
        this.boundingBox = computeBoundingBox();
        this.layer = layer;
        this.tags = tags;
    }

    public int getLayer() {
        return layer;
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

    private BoundingBox computeBoundingBox() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (GraphNode node : geometry) {
            minX = Math.min(minX, node.getX());
            minY = Math.min(minY, node.getY());
            maxX = Math.max(maxX, node.getX());
            maxY = Math.max(maxY, node.getY());
        }

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public abstract void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom);

    public abstract void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom);

    protected String getTagValue(String key) {
        for (Tag t : tags) {
            if (t.getKey().equals(key))
                return t.getValue();
        }

        return null;
    }
}