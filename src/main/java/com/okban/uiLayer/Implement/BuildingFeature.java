package com.okban.uiLayer.Implement;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.model.GraphNode;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class BuildingFeature extends MapFeature {

    public BuildingFeature(List<GraphNode> geometry, int minLOD, int layer, Collection<Tag> tags) {
        super(geometry, minLOD, layer, tags);

    }

    @Override
    public void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom) {

        boolean firstPoint = true;
        double lastX = 0;
        double lastY = 0;
        gc.setLineWidth(1);
        gc.beginPath();

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

        gc.closePath();
        gc.setFill(Color.GRAY);
        gc.fill();

        gc.setStroke(Color.DARKGRAY);
        gc.stroke();

    }

    @Override
    public void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom) {
        String name = getTagValue("name");
        if (name != null && zoom > 2) {

            double maxLength = 0;
            GraphNode bestA = null;
            GraphNode bestB = null;

            for (int i = 0; i < geometry.size() - 1; i++) {

                GraphNode a = geometry.get(i);
                GraphNode b = geometry.get(i + 1);

                double dx = b.getX() - a.getX();
                double dy = b.getY() - a.getY();

                double len = dx * dx + dy * dy;

                if (len > maxLength) {
                    maxLength = len;
                    bestA = a;
                    bestB = b;
                }
            }
            double x = (bestA.getX() + bestB.getX()) / 2;
            double y = (bestA.getY() + bestB.getY()) / 2;
            double dx = bestB.getX() - bestA.getX();
            double dy = bestB.getY() - bestA.getY();

            double angle = Math.toDegrees(Math.atan2(dy, dx));
            if (angle > 90 || angle < -90) {
                angle += 180;
            }
            gc.save();

            gc.translate(
                    (x - cameraX) * zoom,
                    (y - cameraY) * zoom);

            gc.rotate(angle);
            gc.setFont(Font.font(9));
            gc.fillText(name, 0, 0);

            gc.restore();
        }
    }

}
