module com.okban {
    requires javafx.controls;
    requires javafx.fxml;
    requires osmosis.pbf;
    requires osmosis.core;
    requires javafx.graphics;
    requires javafx.base;

    opens com.okban to javafx.fxml;

    exports com.okban;
}
