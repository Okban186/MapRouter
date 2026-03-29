package com.okban.uiLayer;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DataLoadingScreen {

    private ProgressBar progressBar;
    private Label loadingLabel;
    private VBox loadingRoot;

    public void createContent(double ROOT_WIDTH, double ROOT_HEIGH) {
        progressBar = new ProgressBar();
        loadingLabel = new Label("Loading...");

        loadingRoot = new VBox(10, loadingLabel, progressBar);
        loadingRoot.setPrefSize(ROOT_WIDTH, ROOT_HEIGH);
        loadingRoot.setAlignment(Pos.CENTER);
        loadingRoot.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

    }

    public void loadTask(Task loadTask) {
        progressBar.progressProperty().bind(loadTask.progressProperty());
    }

    public Parent getRoot() {
        return loadingRoot;
    }
}
