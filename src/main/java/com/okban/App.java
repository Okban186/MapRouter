package com.okban;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.okban.dto.OsmDataResult;
import com.okban.dto.Pair;
import com.okban.model.GraphNode;
import com.okban.service.OsmFileLoadService;
import com.okban.service.RoutingService;
import com.okban.uiLayer.MapView;
import com.okban.uiLayer.Abstract.MapFeature;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class App extends Application {

    private double ROOT_WIDTH = 1200;
    private double ROOT_HEIGH = 800;
    private AnchorPane root;
    private Pane mapRoot;
    private static Scene scene;
    private String pinIconContent = "M1 0 A1 1 0 1 1 0.999 0 Z";
    private MapView mapView;
    private OsmFileLoadService osmFileLoadService;
    private RoutingService routingService;
    private OsmDataResult osmData;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(createContent());
        stage.setScene(scene);
        stage.setMaxHeight(ROOT_HEIGH);
        stage.setMinHeight(ROOT_HEIGH);
        stage.setMaxWidth(ROOT_WIDTH);
        stage.setMinWidth(ROOT_WIDTH);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public Parent createContent() {
        root = new AnchorPane();
        root.setPrefSize(ROOT_WIDTH, ROOT_HEIGH);
        Button mmb = new Button("mmb");
        mmb.setOnAction(ed -> {
            openPbfFile((Stage) root.getScene().getWindow());
        });
        Button finding = new Button("finding");
        Button hideRoute = new Button("tooglehide");
        hideRoute.setLayoutX(0);
        hideRoute.setLayoutY(100);
        hideRoute.setOnAction(e -> {
            mapView.setRoutingLine(!mapView.getRoutingLine());
        });
        finding.setOnAction(ed -> {
            GraphNode[] graphNodes = osmData.graphNodes;
            // System.out.println(graphNodes[2180420].getLat() + " " +
            // graphNodes[2180420].getLon());
            Task<List<MapFeature>[][]> task = new Task<>() {

                @Override
                protected List<MapFeature>[][] call() throws Exception {
                    // graphNodes[2180420],
                    // graphNodes[2180607],
                    List<Pair<Integer, Integer>> paths = routingService.getRoutingPath(
                            graphNodes[2180420],
                            graphNodes[2180607],
                            graphNodes.length,
                            graphNodes);

                    return routingService.pathToTile(paths, mapView);
                }
            };

            task.setOnSucceeded(e -> {
                List<MapFeature>[][] routeIndexs = task.getValue();
                mapView.setRoutingLine(true);
                mapView.setRoutingTiles(routeIndexs);
            });

            new Thread(task).start();
        });
        finding.setLayoutX(0);
        finding.setLayoutY(50);
        mapView = new MapView(1200, 800);
        osmFileLoadService = new OsmFileLoadService();
        routingService = new RoutingService();
        mapRoot = (Pane) mapView.createMapView();
        root.getChildren().addAll(mapRoot, mmb, finding, hideRoute);
        return root;
    }

    public void openPbfFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Map data save");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pbf file", "*.pbf"));
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            System.out.println("No file selected");
            return;
        }
        Task<OsmDataResult> task = osmFileLoadService.processPbfFile(file, mapView);

        task.setOnSucceeded(e -> {
            osmData = task.getValue();
            mapView.onDataLoaded(osmData);
            mapView.repaint();

        });

        task.setOnFailed(e -> System.out.println("FAILED"));
    }

}
