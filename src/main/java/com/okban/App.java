package com.okban;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.okban.config.MapConfig;
import com.okban.dto.OsmDataResult;
import com.okban.dto.Pair;

import com.okban.model.GraphStorage;
import com.okban.model.SnapContext;
import com.okban.service.OsmFileLoadService;
import com.okban.service.RoutingService;
import com.okban.uiLayer.DataLoadingScreen;
import com.okban.uiLayer.MapView;
import com.okban.uiLayer.SelectedPlacesPane;
import com.okban.uiLayer.Implement.RoutingFeature;

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

    private double ROOT_WIDTH = 1366;
    private double ROOT_HEIGH = 768;
    private AnchorPane root;
    private Pane mapRoot;
    private static Scene scene;

    private MapView mapView;
    private OsmFileLoadService osmFileLoadService;
    private RoutingService routingService;
    private OsmDataResult osmData;
    private DataLoadingScreen loadingScreen;
    private List<SnapContext> snapContexts;
    private SelectedPlacesPane selectedPlacesPane;
    private MapConfig mapConfig;

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

        mapConfig = new MapConfig();

        loadingScreen = new DataLoadingScreen();
        snapContexts = new ArrayList<>();
        loadingScreen.createContent(ROOT_WIDTH, ROOT_HEIGH);

        selectedPlacesPane = new SelectedPlacesPane(snapContexts);

        selectedPlacesPane.setLayoutX(ROOT_WIDTH - selectedPlacesPane.getPrefWidth());
        selectedPlacesPane.setLayoutY(0);

        Button finding = new Button("finding");
        Button hideRoute = new Button("tooglehide");
        hideRoute.setLayoutX(0);
        hideRoute.setLayoutY(100);
        hideRoute.setOnAction(e -> {
            mapView.setRoutingLine(!mapView.getRoutingLine());
            mapView.setNeedRender(true);
        });
        finding.setOnAction(ed -> {
            GraphStorage graphStorage = osmData.graphStorage;

            Task<RoutingFeature> task = new Task<>() {

                @Override
                protected RoutingFeature call() throws Exception {

                    return routingService.GTS();

                }
            };

            task.setOnSucceeded(e -> {
                RoutingFeature routingFeature = task.getValue();
                mapView.setRoutingLine(true);
                mapView.setRoutingTiles(routingFeature);
                System.gc();
                mapView.repaint();
                // Runtime runtime = Runtime.getRuntime();

                // long totalMemory = runtime.totalMemory();
                // long freeMemory = runtime.freeMemory();
                // long usedMemory = totalMemory - freeMemory;

                // System.out.println("Total Memory (MB): " + totalMemory / (1024 * 1024));
                // System.out.println("Free Memory (MB): " + freeMemory / (1024 * 1024));
                // System.out.println("Used Memory (MB): " + usedMemory / (1024 * 1024));
                // System.out.println("Max Memory (MB): " + runtime.maxMemory() / (1024 *
                // 1024));

            });

            new Thread(task).start();
        });
        finding.setLayoutX(0);
        finding.setLayoutY(50);
        mapView = new MapView(1366, 768, mapConfig);
        mapView.setOnPlaceMarker(item -> {
            selectedPlacesPane.addPlace(item);
        });
        selectedPlacesPane.setOnHiddenRoutingPath(hidden -> {
            if (mapView.getRoutingLine()) {
                mapView.setRoutingLine(hidden);
                mapView.setNeedRender(true);
            }
        });

        mapView.setSnapContext(snapContexts);

        osmFileLoadService = new OsmFileLoadService();
        routingService = new RoutingService(snapContexts, mapConfig);

        mapRoot = (Pane) mapView.createMapView();
        root.getChildren().addAll(mapRoot, finding, hideRoute, selectedPlacesPane, loadingScreen.getRoot());//

        openPbfFile();
        return root;
    }

    public void openPbfFile() {

        String documentsDir = System.getenv("XDG_DOCUMENTS_DIR");

        Path path;
        if (documentsDir != null) {
            path = Paths.get(documentsDir, "OsmData");
        } else {
            path = Paths.get(System.getProperty("user.home"), "Documents", "OsmData", "hcm.osm.pbf");
        }

        Task<OsmDataResult> task = osmFileLoadService.processPbfFile(path, mapConfig);
        loadingScreen.loadTask(task);
        task.setOnSucceeded(e -> {
            osmData = task.getValue();
            mapView.onDataLoaded(osmData);
            mapView.repaint();
            root.getChildren().removeLast();
            routingService.setGraphStorage(osmData.graphStorage);
            System.gc();

        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.out.println("Failed");
            if (ex != null)
                ex.printStackTrace();
        });
    }

}
