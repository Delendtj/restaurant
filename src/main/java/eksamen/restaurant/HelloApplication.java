package eksamen.restaurant;
import javafx.application.Application;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.CornerRadii;
import javafx.geometry.Insets;
import java.awt.*;
import java.util.*;

class Order {
    private int orderId;
    private String mealType;
    private int preptime;
    private Customer customer;


    public Order ( int orderId, String mealType, int preptime, Customer customer ) {

        this.orderId = orderId;
        this.mealType = mealType;
        this.preptime = preptime;
        this.customer = customer;
    }

    public int getOrderId() {return orderId;}
    public String getMealType() {return mealType;}
    public int getPreptime() {return preptime;}
    public Customer getCustomer(){return customer;}

}

class OrderQueue {
    private Queue<Order> orders;
    private final int maxOrders;
    private final Object lock = new Object();

    public OrderQueue(int maxOrders) {
    this.maxOrders = maxOrders;
    orders = new LinkedList<>();
    }



}

class Cooks implements Runnable {
    private int cookId;
    private Set<String> mealtype;
    private int OrderQueue;
    private int prepdelay;

    public Cooks(int cookId, Set<String> mealtype, int OrderQueue, int prepdelay) {
        this.cookId = cookId;
        this.mealtype = mealtype;
        this.OrderQueue = OrderQueue;
        this.prepdelay = prepdelay;
    }

    public int getCookId() {
        return cookId;
    }
    public Set<String> getMealtype() {return mealtype;}

    @Override
    public void run() {
        try{
            while(!Thread.currentThread().isInterrupted()) {
                //get order
                //prep meal
                // get customer
                //get delay
            }
        }
        catch (Exception e) {
            Thread.currentThread().interrupt();
        }
}
}

public class HelloApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Restaurant Simulation");

        //Customers
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setBackground(new Background(new BackgroundFill(Color.YELLOW, CornerRadii.EMPTY, Insets.EMPTY)));

        // Order Queue
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(10));
        ListView<String> orderQueue = new ListView<>();
        orderQueue.getItems().addAll(" Order Queue");
        centerPanel.getChildren().add(orderQueue);

        // Cooks
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        rightPanel.getChildren().addAll(new Label("\u231B Cook 1 (Cooking x)"),
                new Label("\u231B Cook 2 (Cooking x)"),
                new Label("\u231B Cook 3 (Cooking x)"));

        //Served
        HBox bottomPanel = new HBox(10);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
        bottomPanel.getChildren().add(new Label(""));


        BorderPane root = new BorderPane();
        root.setLeft(leftPanel);
        root.setCenter(centerPanel);
        root.setRight(rightPanel);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
