package com.okban.uiLayer.Implement;

import java.util.Collection;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.model.GraphStorage;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class BuildingFeature extends MapFeature {

    public BuildingFeature(int segmentOffset, int segmentLen, int minLOD, int layer, double base, int wayflags,
            String name,
            GraphStorage graphStorage) {
        super(segmentOffset, segmentLen, minLOD, layer, base, wayflags, name, graphStorage);

    }

    @Override
    public void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage) {

        boolean firstPoint = true;
        double lastX = 0;
        double lastY = 0;
        gc.save();
        gc.setLineWidth(base);
        gc.beginPath();
        int[] shapeNodes = graphStorage.getShapeNodes();

        for (int i = segmentOffset; i < segmentLen + segmentOffset; i++) {

            double screenX = (graphStorage.getNodeX(shapeNodes[i]) + 512 - cameraX) * zoom;
            double screenY = (graphStorage.getNodeY(shapeNodes[i]) + 512 - cameraY) * zoom;

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
        gc.restore();

    }

    @Override
    public void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage) {

        if (name != null && zoom > 2) {
            gc.save();
            gc.setLineWidth(1);
            gc.setStroke(Color.BLACK);
            double maxLength = 0;

            int bestAIndex = -1;
            int bestBIndex = -1;
            int shapeNodes[] = graphStorage.getShapeNodes();
            for (int i = segmentOffset; i < segmentLen + segmentOffset - 1; i++) {

                int aIndex = shapeNodes[i];
                int bIndex = shapeNodes[i + 1];

                double dx = graphStorage.getNodeX(bIndex) - graphStorage.getNodeX(aIndex);
                double dy = graphStorage.getNodeY(bIndex) - graphStorage.getNodeY(bIndex);

                double len = dx * dx + dy * dy;

                if (len > maxLength) {
                    maxLength = len;
                    bestAIndex = aIndex;
                    bestBIndex = bIndex;
                }
            }
            double ax = graphStorage.getNodeX(bestAIndex);
            double ay = graphStorage.getNodeY(bestAIndex);
            double bx = graphStorage.getNodeX(bestBIndex);
            double by = graphStorage.getNodeY(bestBIndex);

            double x = (ax + bx) / 2;
            double y = (ay + by) / 2;

            double dx = bx - ax;
            double dy = by - ay;

            double angle = Math.toDegrees(Math.atan2(dy, dx));

            if (angle > 90 || angle < -90) {
                angle += 180;
            }

            gc.save();

            double screenX = (x + 512 - cameraX) * zoom;
            double screenY = (y + 512 - cameraY) * zoom;

            gc.translate(screenX, screenY);

            gc.rotate(angle);
            gc.setFont(Font.font(9));
            gc.fillText(name, 0, 0);

            gc.restore();
        }
    }

}
