package com.okban.uiLayer.Implement;

import com.okban.Enum.HighwayType;
import com.okban.Enum.WayFlags;
import com.okban.config.MapConfig;
import com.okban.model.GraphStorage;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RoadFeature extends MapFeature {

    private HighwayType highwayType;

    //label caching
    private double ax, ay;
    private double bx, by;
    private double degree;
    private boolean haveLabelCaching = false;
    private double maxLength = 0;

    public RoadFeature(int segmentOffset, int segmentLen, int minLOD, HighwayType highwayType, int layer, int wayflags,
            String name,
            GraphStorage graphStorage) {

        super(segmentOffset, segmentLen, minLOD, layer, wayflags, name, graphStorage);
        this.highwayType = highwayType;
        

    }

    public double getHighwayWidth() {
        return highwayType.getWidth();
    }

    @Override
    public void draw(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage,
            MapConfig mapConfig) {
        if (segmentLen < 2)
            return;

            if(!haveLabelCaching){
                buildLabelCache(graphStorage);
                haveLabelCaching = true;
        }
        boolean firstPoint = true;
        double lastX = 0;
        double lastY = 0;

        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.beginPath();
        gc.setStroke(Color.web(highwayType.getHexColor()));
        gc.setLineWidth(Math.min(highwayType.getWidth() * zoom, 300));
        int shapeNodes[] = graphStorage.getShapeNodes();
        for (int i = segmentOffset; i < segmentLen + segmentOffset; i++) {

            double screenX = (graphStorage.getNodeX(shapeNodes[i]) + mapConfig.BUFFER / 2 - cameraX) * zoom;
            double screenY = (graphStorage.getNodeY(shapeNodes[i]) + mapConfig.BUFFER / 2 - cameraY) * zoom;

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
private void buildLabelCache(GraphStorage gs) {

    int[] nodes = gs.getShapeNodes();

    double maxLen = 0;
    int bestA = -1, bestB = -1;

    for (int i = segmentOffset; i < segmentOffset + segmentLen - 1; i++) {

        int a = nodes[i];
        int b = nodes[i + 1];

        double dx = gs.getNodeX(b) - gs.getNodeX(a);
        double dy = gs.getNodeY(b) - gs.getNodeY(a);

        double len = dx * dx + dy * dy;

        if (len > maxLen) {
            maxLen = len;
            bestA = a;
            bestB = b;
        }
    }
    maxLength = maxLen;
    ax = gs.getNodeX(bestA);
    ay = gs.getNodeY(bestA);
    bx = gs.getNodeX(bestB);
    by = gs.getNodeY(bestB);

    double angle = Math.atan2(by - ay, bx - ax);
double degree = Math.toDegrees(angle);

if (degree > 90 || degree < -90) {
    degree += 180;
}

this.degree = degree;
    
}
    @Override
    public void drawLabel(GraphicsContext gc, double cameraX, double cameraY, double zoom, GraphStorage graphStorage, MapConfig mapConfig) { 
      
    if (name == null || zoom < 1.5) return;

 

    double x = (ax + bx) * 0.5;
    double y = (ay + by) * 0.5;

    double screenX = (x + mapConfig.BUFFER / 2 - cameraX) * zoom;
    double screenY = (y + mapConfig.BUFFER / 2 - cameraY) * zoom;

    // culling (QUAN TRỌNG)
    if (screenX < -200 || screenX > 2000 || screenY < -200 || screenY > 2000)
        return;
    
    double fontSize = 10; 
    double textWidth = name.length() * (fontSize * 0.55); 
    double segmentLength = Math.sqrt(maxLength) * zoom; 
    if (textWidth > segmentLength) { 
        gc.restore(); 
        return; 
    }
gc.save();
    gc.setFont(Font.font(10));
    gc.setFill(Color.BLACK);

    

    gc.translate(screenX, screenY);
    gc.rotate(degree);

    // gc.fillText(name, 0, 0);
    gc.fillText(name, -textWidth / 2, 0);

    // ARROW (nhẹ hơn bạn nghĩ)
    if ((wayflags & WayFlags.ONEWAY.getValue()) != 0) {
        gc.strokeLine(20, 0, 35, 0);
        gc.strokeLine(35, 0, 30, -3);
        gc.strokeLine(35, 0, 30, 3);
    }

    gc.restore();
}
}
