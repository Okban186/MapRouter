package com.okban.uiLayer;

import java.util.List;

import com.okban.model.SnapContext;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SelectedPlacesPane extends VBox {

    private final ObservableList<String> places;
    private final ListView<String> listView;
    private final TitledPane titledPane;
    private List<SnapContext> snapContexts;

    public SelectedPlacesPane(List<SnapContext> snapContexts) {
        this.snapContexts = snapContexts;
        places = FXCollections.observableArrayList();

        listView = new ListView<>(places);
        //
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

        this.getChildren().add(titledPane);
        this.setPrefSize(400, 200);
    }

    public void addPlace(String place) {
        if (!places.contains(place)) {
            places.add(place);
        }
    }

    public void clearPlaces() {
        places.clear();
    }

    public ObservableList<String> getPlaces() {
        return places;
    }
}
