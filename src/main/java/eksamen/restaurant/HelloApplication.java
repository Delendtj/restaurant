package eksamen.restaurant;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
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
    private int maxOrders;
    private final Object lock = new Object();
    private ObservableList<String> uiOrderList;

    public OrderQueue(int maxOrders, ObservableList<String> uiOrderList) {
    this.maxOrders = maxOrders;
    orders = new LinkedList<>();
    this.uiOrderList = uiOrderList;
    }
    private void addInitialOrder(Order order) {
        if (this.orders.size() < this.maxOrders) {
            this.orders.offer(order);
            System.out.println(" -> Added initial order: ID " + order.getOrderId() + " (" + order.getMealType() + ")");
        } else {
            System.out.println(" -> Queue full (max=" + this.maxOrders + "). Could not add initial order ID " + order.getOrderId());
        }
    }
    public Order getOrder(Set<String> cookMealTypes) throws InterruptedException {
        synchronized (lock) {
            while (orders.isEmpty()) {
                lock.wait(); // If no orders, wait
            }

            Order order = orders.peek();
            if (cookMealTypes.contains(order.getMealType())) {
                orders.poll();
                updateGuiList();
                return order;
            } else {
                return null;
            }
        }
    }
    private void updateGuiList(){
        if (uiOrderList != null) {
            Platform.runLater(() -> {
                uiOrderList.clear();
                for (Order o : orders) {
                    uiOrderList.add("Order " + o.getOrderId() + ": " + o.getMealType() + " for " + o.getCustomer().getName());
                }
            });
        }
    }
    public void increaseMaxOrders(){
        maxOrders++;
        System.out.println(" -> Increase max orders: " + maxOrders);
    }
    public void decreaseMaxOrders(){
        maxOrders--;
        System.out.println(" -> Decrease max orders to: " + maxOrders);
    }
}

class Cooks implements Runnable {
    private final int cookId;
    private final Set<String> mealtype;
    private final OrderQueue orderQueue;
    private final int prepdelay;
    private final ObservableList<String> servedOrdersList;
    private final Label cookstatusLabel;

    public Cooks(int cookId, Set<String> mealtype, OrderQueue orderQueue, int prepdelay, ObservableList<String> servedOrdersList, Label cookstatusLabel) {
        this.cookId = cookId;
        this.mealtype = mealtype;
        this.orderQueue = orderQueue;
        this.prepdelay = prepdelay;
        this.servedOrdersList = servedOrdersList;
        this.cookstatusLabel = cookstatusLabel;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Order order = orderQueue.getOrder(mealtype);
                if (order != null) {
                    System.out.println("Cook " + cookId + " processing order " + order.getOrderId() + ": " + order.getMealType() + " for Customer " + order.getCustomer().getName());
                    Platform.runLater(() -> cookstatusLabel.setText("Cooking: " + order.getMealType() + " for " + order.getCustomer().getName()));

                    try {
                        long timeToPrepare = order.getPreptime() * prepdelay;
                        Thread.sleep(timeToPrepare);
                        Platform.runLater(() -> {
                            servedOrdersList.add("Order " + order.getOrderId() + ": " + order.getMealType() + " for " + order.getCustomer().getName() + " served!");
                            cookstatusLabel.setText("Idle");
                        });
                    } catch (InterruptedException e) {
                        System.out.println("Cook " + cookId + " interrupted while cooking order " + order.getOrderId());
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }
}



public class HelloApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Restaurant Simulation");
        ObservableList<String> uiOrderList = FXCollections.observableArrayList();
        ObservableList<String> servedOrdersList = FXCollections.observableArrayList();
        OrderQueue orderQueue = new OrderQueue(5, uiOrderList);


        Set<String> cook1Meals = new HashSet<>(List.of("Burger"));
        Set<String> cook2Meals = new HashSet<>(List.of("Fries"));
        Set<String> cook3Meals = new HashSet<>(List.of("Salad"));

        Label cook1Status = new Label("idle");
        Label cook2Status = new Label("idle");
        Label cook3Status = new Label("idle");
        // Start cooks in separate threads
        Thread cook1 = new Thread(new Cooks(1, cook1Meals, orderQueue, 5, servedOrdersList, cook1Status));
        Thread cook2 = new Thread(new Cooks(2, cook2Meals, orderQueue, 5, servedOrdersList, cook2Status));
        Thread cook3 = new Thread(new Cooks(3, cook3Meals, orderQueue, 5, servedOrdersList, cook3Status));

        cook1.start();
        cook2.start();
        cook3.start();



        Button increaseMaxOrderButton = new Button ("Increase Max acceptable orders in queue");
        increaseMaxOrderButton.setOnAction (e-> orderQueue.increaseMaxOrders());

        Button decreaseMaxOrderButton = new Button ("Decrease Max acceptable orders in queue");
        decreaseMaxOrderButton.setOnAction (e-> orderQueue.decreaseMaxOrders());

        // --- UI Setup ---
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setBackground(new Background(new BackgroundFill(Color.YELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
        leftPanel.getChildren().add(new Label("👤 Customers"));

        // Center - Order Queue (binds to observable list)
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(10));
        ListView<String> orderQueueView = new ListView<>(uiOrderList);
        centerPanel.getChildren().addAll(new Label("🧾 Order Queue"), orderQueueView);
        centerPanel.getChildren().add(increaseMaxOrderButton);
        centerPanel.getChildren().add(decreaseMaxOrderButton);

        // Right - Cooks (static labels for now)
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        rightPanel.getChildren().addAll(
                new Label("⏳ Cook 1"), cook1Status,
                new Label("⏳ Cook 2"), cook2Status,
                new Label("⏳ Cook 3"), cook3Status
        );

        // Bottom - Served Orders
        HBox bottomPanel = new HBox(5);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN, CornerRadii.EMPTY, Insets.EMPTY)));
        ListView<String> servedOrdersView = new ListView<>(servedOrdersList);
        bottomPanel.getChildren().addAll(new Label("✅ Served Orders"), servedOrdersView);


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
