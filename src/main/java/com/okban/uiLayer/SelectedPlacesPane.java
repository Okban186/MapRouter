package com.okban.uiLayer;

import java.util.List;
import java.util.function.Consumer;

import com.okban.Enum.VehicleType;
import com.okban.config.MapConfig;
import com.okban.model.SnapContext;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SelectedPlacesPane extends VBox {

    private final ObservableList<String> places;
    private final ListView<String> listView;
    private final TitledPane titledPane;
    private List<SnapContext> snapContexts;
    private Consumer<Boolean> onHiddenRoutingPath;

    private String currentVehicle = "car";

    public SelectedPlacesPane(List<SnapContext> snapContexts) {
        this.snapContexts = snapContexts;
        places = FXCollections.observableArrayList();

        listView = new ListView<>(places);
        listView.setCellFactory(param -> {
            ListCell<String> cell = new ListCell<>() {
                private final HBox content = new HBox(10);
                private final Label nameLabel = new Label();
                private final Button removeBtn = new Button("X");

                {
                    removeBtn.setOnAction(e -> {
                        int idx = getIndex();
                        if (idx >= 0 && idx < places.size()) {
                            places.remove(idx);
                            snapContexts.remove(idx);
                            MapConfig.snapcontextsChanged = true;
                        }
                    });
                    content.getChildren().addAll(nameLabel, removeBtn);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : content);
                    if (!empty)
                        nameLabel.setText(item);
                }
            };

            // Drag & Drop
            cell.setOnDragDetected(event -> {
                if (cell.isEmpty())
                    return;
                var db = cell.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                var content = new javafx.scene.input.ClipboardContent();
                content.putString(cell.getItem());
                db.setContent(content);
                event.consume();
            });

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                }
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                var db = event.getDragboard();
                if (!db.hasString())
                    return;

                int draggedIdx = listView.getItems().indexOf(db.getString());
                int thisIdx = cell.getIndex();
                if (draggedIdx == thisIdx)
                    return;
                if (thisIdx == 0)
                    MapConfig.snapcontextsChanged = true;
                String item = places.remove(draggedIdx);
                places.add(thisIdx, item);

                SnapContext snap = snapContexts.remove(draggedIdx);
                snapContexts.add(thisIdx, snap);

                listView.getSelectionModel().select(thisIdx);
                event.setDropCompleted(true);
                event.consume();
            });

            return cell;
        });

        titledPane = new TitledPane("Địa điểm đã chọn", listView);
        titledPane.setCollapsible(true);

        HBox vehicleBox = new HBox(10);
        ToggleGroup vehicleGroup = new ToggleGroup();

        RadioButton carBtn = new RadioButton("Ô tô");
        carBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        carBtn.setToggleGroup(vehicleGroup);
        carBtn.setSelected(true);
        carBtn.setOnAction(e -> {
            currentVehicle = "car";
            MapConfig.currentVehicleType = VehicleType.CAR;
            onHiddenRoutingPath.accept(false);
        });

        RadioButton motorBtn = new RadioButton("Xe máy");
        motorBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        motorBtn.setToggleGroup(vehicleGroup);
        motorBtn.setOnAction(e -> {
            currentVehicle = "motorcycle";
            MapConfig.currentVehicleType = VehicleType.MOTORCYCLE;
            onHiddenRoutingPath.accept(false);
        });

        RadioButton busBtn = new RadioButton("Phương tiện dịch vụ công cộng");
        busBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        busBtn.setToggleGroup(vehicleGroup);
        busBtn.setOnAction(e -> {
            currentVehicle = "PSV";
            MapConfig.currentVehicleType = VehicleType.PSV;
            onHiddenRoutingPath.accept(false);
        });

        vehicleBox.getChildren().addAll(carBtn, motorBtn, busBtn);

        this.getChildren().addAll(titledPane, vehicleBox);
        this.setPrefSize(400, 250);
    }

    public void addPlace(String place) {
        places.add(place);
        MapConfig.snapcontextsChanged = true;
    }

    public void clearPlaces() {
        places.clear();
        snapContexts.clear();
    }

    public ObservableList<String> getPlaces() {
        return places;
    }

    public String getCurrentVehicle() {
        return currentVehicle;
    }

    public void setOnHiddenRoutingPath(Consumer<Boolean> onHiddenRoutingPath) {
        this.onHiddenRoutingPath = onHiddenRoutingPath;
    }
}