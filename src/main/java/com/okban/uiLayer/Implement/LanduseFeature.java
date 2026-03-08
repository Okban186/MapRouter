package com.okban.uiLayer.Implement;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.model.GraphNode;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class LanduseFeature extends MapFeature {

    public LanduseFeature(List<GraphNode> geometry, int minLOD, int layer, Collection<Tag> tags) {
        super(geometry, minLOD, layer, tags);
    }

    @Override
    public void draw(GraphicsContext gc,
            double cameraX,
            double cameraY,
            double zoom) {

        boolean firstPoint = true;
        double lastX = 0;
        double lastY = 0;

        gc.beginPath();

        for (int i = 0; i < geometry.size(); i++) {

            GraphNode n = geometry.get(i);

            double screenX = (n.getX() - cameraX) * zoom;
            double screenY = (n.getY() - cameraY) * zoom;

            if (zoom < 0.5 && !firstPoint &&
                    Math.abs(screenX - lastX) < 1 &&
                    Math.abs(screenY - lastY) < 1) {
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

        gc.closePath();

        gc.setFill(Color.web("#d8e8c8"));
        gc.fill();
    }

    @Override
    public void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom) {
        return;
    }
}