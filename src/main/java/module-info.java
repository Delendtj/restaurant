module eksamen.restaurant {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens eksamen.restaurant to javafx.fxml;
    exports eksamen.restaurant;
}