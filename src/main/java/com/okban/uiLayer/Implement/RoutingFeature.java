package com.okban.uiLayer.Implement;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.okban.Enum.WayFlags;
import com.okban.dto.DijkstraResult;
import com.okban.dto.Pair;
import com.okban.model.GraphStorage;
import com.okban.model.SnapContext;

import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RoutingFeature {

    private Color mainColor;
    private int base;
    private List<DijkstraResult> geo;
    protected int minLOD;
    protected BoundingBox boundingBox;
    protected int layer;
    protected Collection<Tag> tags;
    protected int lastDrawFrame = -1;
    protected int maxTileX;
    protected int minTileX;
    protected int maxTileY;
    protected int minTileY;
    private double cost;

    public RoutingFeature(int minLOD, Color mainColor, int base, GraphStorage graphStorage, List<DijkstraResult> geo,
            BoundingBox boundingBox, int maxTileX, int minTileX, int maxTileY, int minTileY, double cost) {
        this.mainColor = mainColor;
        this.base = base;
        this.geo = geo;
        this.minLOD = minLOD;
        this.base = base;
        this.boundingBox = boundingBox;
        this.maxTileX = maxTileX;
        this.minTileX = minTileX;
        this.maxTileY = maxTileY;
        this.minTileY = minTileY;
        this.cost = cost;
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

    public int getMinTileX() {
        return minTileX;
    }

    public int getMaxTileX() {
        return maxTileX;
    }

    public int getMinTileY() {
        return minTileY;
    }

    public int getMaxTileY() {
        return maxTileY;
    }

    public void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage) {
        if (geo.size() < 2)
            return;

        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.beginPath();
        gc.setStroke(mainColor);
        gc.setLineWidth(Math.max(5, Math.min(base * zoom, 60)));
        int number = 1;
        double lastX = 0;
        double lastY = 0;
        boolean hasStarted = false;
        int shapeNodes[] = graphStorage.getShapeNodes();
        for (DijkstraResult dResult : geo) {

            SnapContext snap1 = dResult.start;
            SnapContext snap2 = dResult.end;

            double x1 = (snap1.getX() + 512 - cameraX) * zoom;
            double y1 = (snap1.getY() + 512 - cameraY) * zoom;

            if (!hasStarted) {
                gc.moveTo(x1, y1);
                hasStarted = true;
                gc.save();
                gc.setFill(Color.RED);

                gc.fillOval(x1, y1, 4, 4);
                gc.restore();
                gc.save();
                gc.setFill(Color.RED); // màu khác
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                gc.fillText(String.valueOf(number++), x1 - 40, y1 - 4);

                gc.restore();
            } else {
                gc.lineTo(x1, y1);
            }

            lastX = x1;
            lastY = y1;
            List<Pair<Integer, Integer>> dPairs = dResult.path;
            if (dPairs != null) {
                if (snap1.getNode1() == snap2.getNode1() && snap1.getNode2() == snap2.getNode2()
                        && (snap1.getWayflags() & WayFlags.ONEWAY.getValue()) == 0) {
                    for (int i = 0; i < dPairs.size(); i++) {
                        double x = (graphStorage.getNodeX(dPairs.get(i).getKey()) + 512 - cameraX) * zoom;
                        double y = (graphStorage.getNodeY(dPairs.get(i).getKey()) + 512 - cameraY) * zoom;

                        double dx = x - lastX;
                        double dy = y - lastY;

                        if (zoom < 1.5 && dx * dx + dy * dy < 4) {
                            continue;
                        }

                        gc.lineTo(x, y);

                        lastX = x;
                        lastY = y;
                    }
                } else {

                    int node1 = dPairs.getFirst().key;

                    if (node1 == snap1.getNode1()) {
                        if (node1 != snap1.getNearest1()) {
                            int ai = snap1.getOffset1();
                            while (true) {
                                int nodeIndex = shapeNodes[ai];
                                if (node1 == nodeIndex)
                                    break;
                                double x = (graphStorage.getNodeX(nodeIndex) + 512 - cameraX) * zoom;
                                double y = (graphStorage.getNodeY(nodeIndex) + 512 - cameraY) * zoom;

                                gc.lineTo(x, y);

                                lastX = x;
                                lastY = y;

                                ai--;
                            }
                        }
                    } else if (node1 == snap1.getNode2()) {
                        if (node1 != snap1.getNearest2()) {
                            int bi = snap1.getOffset2();
                            while (true) {
                                int nodeIndex = shapeNodes[bi];
                                if (node1 == nodeIndex)
                                    break;
                                double x = (graphStorage.getNodeX(nodeIndex) + 512 - cameraX) * zoom;
                                double y = (graphStorage.getNodeY(nodeIndex) + 512 - cameraY) * zoom;

                                gc.lineTo(x, y);

                                lastX = x;
                                lastY = y;

                                bi++;
                            }
                        }
                    }

                    for (int index = 1; index < dPairs.size(); index++) {
                        Integer edgeId = dPairs.get(index).getValue();
                        if (edgeId == null)
                            continue;
                        int[] shapeNodeIds = graphStorage.getShapeNodeIds(edgeId);

                        if (shapeNodeIds.length == 0)
                            continue;

                        boolean reverse = graphStorage.isReverse(edgeId);
                        boolean isOneWay = (graphStorage.getWayflag(edgeId) & WayFlags.ONEWAY.getValue()) != 0;

                        int startIdx = reverse ? shapeNodeIds.length - 1 : 0;
                        int endIdx = reverse ? -1 : shapeNodeIds.length;
                        int step = reverse ? -1 : 1;

                        for (int i = startIdx; i != endIdx; i += step) {

                            double x = (graphStorage.getNodeX(shapeNodeIds[i]) + 512 - cameraX) * zoom;
                            double y = (graphStorage.getNodeY(shapeNodeIds[i]) + 512 - cameraY) * zoom;

                            double dx = x - lastX;
                            double dy = y - lastY;

                            if (zoom < 1.5 && dx * dx + dy * dy < 4) {
                                continue;
                            }

                            gc.lineTo(x, y);

                            if (isOneWay) {
                                drawArrow(gc, lastX, lastY, x, y, zoom);
                            }

                            lastX = x;
                            lastY = y;
                        }
                    }

                    int node2 = dPairs.getLast().key;

                    if (node2 == snap2.getNode1()) {
                        if (node2 != snap2.getNearest1()) {

                            for (int ai = snap2.getLen1(); ai >= 0; ai--) {
                                int nodeIndex = shapeNodes[snap2.getOffset1() - ai];
                                if (node2 == snap2.getNearest1())
                                    break;
                                double x = (graphStorage.getNodeX(nodeIndex) + 512 - cameraX) * zoom;
                                double y = (graphStorage.getNodeY(nodeIndex) + 512 - cameraY) * zoom;

                                gc.lineTo(x, y);

                                lastX = x;
                                lastY = y;

                            }
                        }
                    } else if (node2 == snap2.getNode2()) {
                        if (node2 != snap2.getNearest2()) {

                            for (int bi = snap2.getLen2(); bi >= 0; bi--) {
                                int nodeIndex = shapeNodes[snap2.getOffset2() + bi];
                                if (node2 == snap2.getNearest2())
                                    break;
                                double x = (graphStorage.getNodeX(nodeIndex) + 512 - cameraX) * zoom;
                                double y = (graphStorage.getNodeY(nodeIndex) + 512 - cameraY) * zoom;

                                gc.lineTo(x, y);

                                lastX = x;
                                lastY = y;

                            }
                        }
                    }
                }
            }
            double x2 = (snap2.getX() + 512 - cameraX) * zoom;
            double y2 = (snap2.getY() + 512 - cameraY) * zoom;
            gc.lineTo(x2, y2);
            gc.save();
            gc.setFill(Color.RED);

            gc.fillOval(x2, y2, 4, 4);
            gc.restore();
            gc.save();
            gc.setFill(Color.RED); // màu khác
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            gc.fillText(String.valueOf(number++), x2 + 20, y2 - 4);

            gc.restore();

            lastX = x2;
            lastY = y2;

        }

        gc.stroke();
        gc.restore();

        gc.save();

        gc.setFill(Color.WHITE);
        gc.fillRoundRect(lastX + 30, lastY - 20, 100, 20, 8, 8);

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.fillText(String.format("%.1f m", cost), lastX + 35, lastY - 5);

        gc.restore();

    }

    private void drawArrow(GraphicsContext gc,
            double x1, double y1,
            double x2, double y2,
            double zoom) {
        gc.save();
        gc.setFill(Color.WHITE);
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);

        if (len < 10)
            return;

        dx /= len;
        dy /= len;

        double mx = (x1 + x2) / 2;
        double my = (y1 + y2) / 2;

        double size = Math.max(4, Math.min(zoom * 4, 10));

        double px = -dy;
        double py = dx;

        double ax1 = mx - dx * size + px * size * 0.5;
        double ay1 = my - dy * size + py * size * 0.5;

        double ax2 = mx - dx * size - px * size * 0.5;
        double ay2 = my - dy * size - py * size * 0.5;

        gc.strokeLine(mx, my, ax1, ay1);
        gc.strokeLine(mx, my, ax2, ay2);

        gc.restore();
    }

}
