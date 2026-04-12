package com.okban.uiLayer.Implement;

import com.okban.Enum.WayFlags;
import com.okban.config.MapConfig;
import com.okban.model.GraphStorage;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;

public class BuildingFeature extends MapFeature {

    private double base;

    public BuildingFeature(int segmentOffset, int segmentLen, int minLOD, int layer, double base, int wayflags,
            String name,
            GraphStorage graphStorage) {
        super(segmentOffset, segmentLen, minLOD, layer, wayflags, name, graphStorage);
        this.base = base;

    }

    @Override
    public void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage,
            MapConfig mapConfig) {
        boolean firstPoint = true;
        double lastX = 0;
        double lastY = 0;
        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(base);
        gc.beginPath();
        int[] shapeNodes = graphStorage.getShapeNodes();

        for (int i = segmentOffset; i < segmentLen + segmentOffset; i++) {

            double screenX = (graphStorage.getNodeX(shapeNodes[i]) + 512 - cameraX) * zoom;
            double screenY = (graphStorage.getNodeY(shapeNodes[i]) + 512 - cameraY) * zoom;

            double dx = screenX - lastX;
            double dy = screenY - lastY;

            if (zoom < 1.8 && !firstPoint && dx * dx + dy * dy < 8) {
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
        gc.setStroke(Color.web("#CBCBCB"));
        if ((wayflags & WayFlags.BUILDING.getValue()) != 0) {
            gc.setFill(Color.web("#CBCBCB"));
            gc.fill();
            if ((wayflags & WayFlags.HISTORIC.getValue()) != 0 || (wayflags & WayFlags.TOURISM.getValue()) != 0) {
                gc.setLineWidth(1);
                gc.setStroke(Color.web("#0A5C36"));
            }
        } else {
            gc.setLineWidth(1);
            gc.setStroke(Color.web("#0A5C36"));
        }

        gc.stroke();
        gc.restore();

    }

    @Override
    public void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage,
            MapConfig mapConfig) {

        if (name != null && zoom > 1.5) {
            gc.save();

            gc.setFont(Font.font(9));
            gc.setFill(Color.RED);

            double x = (boundingBox.getMinX() + boundingBox.getMaxX()) / 2;
            double y = (boundingBox.getMinY() + boundingBox.getMaxY()) / 2;

            double screenX = (x + 512 - cameraX) * zoom;
            double screenY = (y + 512 - cameraY) * zoom;

            double textWidth = name.length() * 5;
            double textHeight = 9;

            gc.fillText(name,
                    screenX - textWidth / 2,
                    screenY + textHeight / 4);

            gc.restore();
        }
    }

}
