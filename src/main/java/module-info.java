module eksamen.restaurant {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens eksamen.restaurant to javafx.fxml;
    exports eksamen.restaurant;
}