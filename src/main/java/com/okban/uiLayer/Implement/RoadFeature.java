package com.okban.uiLayer.Implement;

import java.util.Collection;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.Enum.WayFlags;
import com.okban.model.GraphStorage;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RoadFeature extends MapFeature {

    public RoadFeature(int segmentOffset, int segmentLen, int minLOD, double base, int layer, int wayflags, String name,
            GraphStorage graphStorage) {
        super(segmentOffset, segmentLen, minLOD, layer, base, wayflags, name, graphStorage);

    }

    @Override
    public void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage) {
        if (segmentLen < 2)
            return;
        boolean firstPoint = true;
        double lastX = 0;
        double lastY = 0;

        gc.save();
        gc.beginPath();
        gc.setStroke(Color.DARKGREY);
        gc.setLineWidth(Math.min(base * zoom, 60));
        int shapeNodes[] = graphStorage.getShapeNodes();
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

        gc.stroke();
        gc.restore();
    }

    @Override
    public void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage) {

        if (name != null && zoom > 2) {
            gc.setLineWidth(1);
            gc.save();
            gc.setStroke(Color.AQUA);
            gc.setFill(Color.RED);
            double maxLength = 0;

            int bestAIndex = -1;
            int bestBIndex = -1;
            boolean flipped = false;
            int shapeNodes[] = graphStorage.getShapeNodes();
            for (int i = segmentOffset; i < segmentLen + segmentOffset - 1; i++) {

                int aIndex = shapeNodes[i];
                int bIndex = shapeNodes[i + 1];

                double dx = graphStorage.getNodeX(bIndex) - graphStorage.getNodeX(aIndex);
                double dy = graphStorage.getNodeY(bIndex) - graphStorage.getNodeY(aIndex);

                double len = dx * dx + dy * dy;

                if (len > maxLength) {
                    maxLength = len;
                    bestAIndex = aIndex;
                    bestBIndex = bIndex;
                }
            }
            if (bestAIndex < 0 || bestBIndex < 0)
                return;

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

                flipped = true;
            }
            gc.save();

            double screenX = (x + 512 - cameraX) * zoom;
            double screenY = (y + 512 - cameraY) * zoom;

            gc.translate(
                    screenX,
                    screenY);

            gc.rotate(angle);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));

            gc.fillText(name, 0, 0);

            if ((wayflags & WayFlags.ONEWAY.getValue()) != 0) {
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
            }
            gc.restore();

        }
    }

}
