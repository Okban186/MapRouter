package com.okban.uiLayer.Implement;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.dto.OsmNode;
import com.okban.model.GraphNode;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class RoadFeature extends MapFeature {
    private double base;

    public RoadFeature(List<GraphNode> geometry, int minLOD, double base, int layer, Collection<Tag> tags) {
        super(geometry, minLOD, layer, tags);
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
        gc.setStroke(Color.DARKGREY);
        gc.setLineWidth(Math.min(base * zoom, 60));

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
        String name = getTagValue("name");
        if (name != null && zoom > 2) {
            gc.setLineWidth(1);
            gc.setStroke(Color.BLACK);
            double maxLength = 0;
            GraphNode bestA = null;
            GraphNode bestB = null;
            boolean flipped = false;
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

                flipped = true;
            }
            gc.save();

            gc.translate(
                    (x - cameraX) * zoom,
                    (y - cameraY) * zoom);

            gc.rotate(angle);
            gc.setFont(Font.font(9));
            gc.fillText(name, 0, 0);
            double arrowLen = 12;
            double arrowWing = 4;

            double dir = flipped ? -1 : 1;

            gc.strokeLine(0, 0, dir * arrowLen, 0);

            gc.strokeLine(
                    dir * arrowLen, 0,
                    dir * (arrowLen - arrowWing), -arrowWing);

            gc.strokeLine(
                    dir * arrowLen, 0,
                    dir * (arrowLen - arrowWing), arrowWing);
            gc.restore();
        }
    }

}
