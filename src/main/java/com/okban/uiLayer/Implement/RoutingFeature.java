package com.okban.uiLayer.Implement;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.model.GraphNode;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class RoutingFeature extends MapFeature {

    private Color mainColor;
    private int base;

    public RoutingFeature(List<GraphNode> geometry, int minLOD, int layer, Collection<Tag> tags, Color mainColor,
            int base) {
        super(geometry, minLOD, layer, tags);
        this.mainColor = mainColor;
        this.base = base;
    }

    @Override
    public void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom) {
        if (geometry.size() < 2)
            return;
        boolean firstPoint = true;
        double lastX = 0;
        double lastY = 0;

        gc.beginPath();
        gc.setStroke(mainColor);
        gc.setLineWidth(Math.max(5, Math.min(base * zoom, 60)));

        for (int i = 0; i < geometry.size(); i++) {

            GraphNode n = geometry.get(i);

            double screenX = (n.getX() - cameraX) * zoom;
            double screenY = (n.getY() - cameraY) * zoom;

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

    @Override
    public void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom) {

    }

}
