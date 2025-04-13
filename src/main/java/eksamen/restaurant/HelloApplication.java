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
import java.util.concurrent.ConcurrentHashMap;

class Customer {
    private int customerId;
    private String name;

    public Customer(int customerId) {
        this.customerId = customerId;
        this.name = "Customer " + customerId;
    }

    public Customer(int customerId, String name) {
        this.customerId = customerId;
        this.name = name;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " (ID: " + customerId + ")";
    }
}

class CustomerTask implements Runnable {
    private Customer customer;
    private OrderQueue orderQueue;
    private ObservableList<String> customerList;
    private static final String[] MEALS = {"Burger", "Fries", "Salad"};
    private static final Random random = new Random();

    // Map to track orders and their completion status
    private static final Map<Integer, Boolean> completedOrders = new ConcurrentHashMap<>();

    public CustomerTask(Customer customer, OrderQueue orderQueue, ObservableList<String> customerList) {
        this.customer = customer;
        this.orderQueue = orderQueue;
        this.customerList = customerList;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(3000 + random.nextInt(3000)); // 3–6 seconds wait

                String meal = MEALS[random.nextInt(MEALS.length)];
                int prepTime = switch (meal) {
                    case "Burger" -> 2000;
                    case "Fries" -> 1500;
                    case "Salad" -> 1000;
                    default -> 2000;
                };

                int orderId = getNextOrderId();
                Order order = new Order(orderId, meal, prepTime, customer);

                // Initialize this order as not completed
                completedOrders.put(orderId, false);

                Platform.runLater(() -> {
                    if (!customerList.contains(customer.toString())) {
                        customerList.add(customer.toString());
                    }
                });

                // Add the order to the queue
                synchronized (orderQueue) {
                    orderQueue.addOrder(order);
                }

                System.out.println(customer.getName() + " la inn en ny bestilling: " + meal);

                // Start timing - customer waits for order
                long startTime = System.currentTimeMillis();
                long maxWaitTime = 10000; // 10 seconds max wait
                boolean orderReceived = false;

                // Wait until either order is complete or timeout
                while ((System.currentTimeMillis() - startTime) < maxWaitTime) {
                    // Check if order has been completed
                    if (completedOrders.getOrDefault(orderId, false)) {
                        orderReceived = true;
                        break;
                    }
                    Thread.sleep(100); // Small sleep to avoid CPU spinning
                }

                // Handle result based on whether order was received
                if (orderReceived) {
                    System.out.println("😊 " + customer.getName() + " fikk maten i tide og er fornøyd!");
                    Platform.runLater(() -> customerList.remove(customer.toString()));
                } else {
                    // Customer leaves without food - cancel the order
                    System.out.println("😠 " + customer.getName() + " ble utålmodig og gikk sin vei!");
                    orderQueue.cancelOrder(orderId);
                    Platform.runLater(() -> customerList.remove(customer.toString()));
                    break; // Customer leaves
                }

                // Clean up the completed order tracking
                completedOrders.remove(orderId);
            }
        } catch (InterruptedException e) {
            Platform.runLater(() -> customerList.remove(customer.toString()));
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static int nextOrderId = 1;

    private static synchronized int getNextOrderId() {
        return nextOrderId++;
    }

    // Method for cooks to mark an order as completed
    public static void markOrderCompleted(int orderId) {
        completedOrders.put(orderId, true);
    }
}

class Order {
    private int orderId;
    private String mealType;
    private int preptime;
    private Customer customer;
    private boolean cancelled;

    public Order(int orderId, String mealType, int preptime, Customer customer) {
        this.orderId = orderId;
        this.mealType = mealType;
        this.preptime = preptime;
        this.customer = customer;
        this.cancelled = false;
    }

    public int getOrderId() {return orderId;}
    public String getMealType() {return mealType;}
    public int getPreptime() {return preptime;}
    public Customer getCustomer() {return customer;}
    public boolean isCancelled() {return cancelled;}
    public void setCancelled(boolean cancelled) {this.cancelled = cancelled;}
}

class OrderQueue {
    private Queue<Order> orders;
    private int maxOrders;
    private final Object lock = new Object();
    private ObservableList<String> uiOrderList;
    private Set<Integer> cancelledOrders = new HashSet<>();

    public OrderQueue(int maxOrders, ObservableList<String> uiOrderList) {
        this.maxOrders = maxOrders;
        orders = new LinkedList<>();
        this.uiOrderList = uiOrderList;
    }

    public Order getOrder(Set<String> cookMealTypes) throws InterruptedException {
        synchronized (lock) {
            while (true) {
                while (orders.isEmpty()) {
                    lock.wait();
                }

                Iterator<Order> iterator = orders.iterator();
                while (iterator.hasNext()) {
                    Order order = iterator.next();

                    // Skip cancelled orders
                    if (cancelledOrders.contains(order.getOrderId())) {
                        iterator.remove();
                        cancelledOrders.remove(order.getOrderId());
                        updateGuiList();
                        continue;
                    }

                    if (cookMealTypes.contains(order.getMealType())) {
                        iterator.remove(); // found match
                        updateGuiList();
                        lock.notifyAll(); // notify customers waiting
                        return order;
                    }
                }

                // No suitable order found, wait for something new
                lock.wait();
            }
        }
    }

    public void addOrder(Order order) throws InterruptedException {
        synchronized (lock) {
            while (orders.size() >= maxOrders) {
                System.out.println("Køen er full – " + order.getCustomer().getName() + " venter...");
                lock.wait(); // vent til det er plass
            }

            orders.offer(order);
            System.out.println(" -> Ny bestilling: ID " + order.getOrderId() + " (" + order.getMealType() + ")");
            updateGuiList();
            lock.notifyAll();
        }
    }

    public void cancelOrder(int orderId) {
        synchronized (lock) {
            cancelledOrders.add(orderId);
            System.out.println(" -> Order " + orderId + " cancelled by customer");
            // We'll actually remove the order next time getOrder is called
            lock.notifyAll();
        }
    }

    private void updateGuiList() {
        if (uiOrderList != null) {
            Platform.runLater(() -> {
                uiOrderList.clear();
                for (Order o : orders) {
                    uiOrderList.add("Order " + o.getOrderId() + ": " + o.getMealType() + " for " + o.getCustomer().getName());
                }
            });
        }
    }

    public void increaseMaxOrders() {
        maxOrders++;
        System.out.println(" -> Increase max orders: " + maxOrders);
    }

    public void decreaseMaxOrders() {
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

                        // Order is now ready - notify the customer
                        CustomerTask.markOrderCompleted(order.getOrderId());

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
        ObservableList<String> currentCustomersList = FXCollections.observableArrayList();
        OrderQueue orderQueue = new OrderQueue(5, uiOrderList);

        Set<String> cook1Meals = new HashSet<>(List.of("Burger"));
        Set<String> cook2Meals = new HashSet<>(List.of("Fries"));
        Set<String> cook3Meals = new HashSet<>(List.of("Salad"));

        Label cook1Status = new Label("Idle");
        Label cook2Status = new Label("Idle");
        Label cook3Status = new Label("Idle");
        // Start cooks in separate threads
        Thread cook1 = new Thread(new Cooks(1, cook1Meals, orderQueue, 5, servedOrdersList, cook1Status));
        Thread cook2 = new Thread(new Cooks(2, cook2Meals, orderQueue, 5, servedOrdersList, cook2Status));
        Thread cook3 = new Thread(new Cooks(3, cook3Meals, orderQueue, 5, servedOrdersList, cook3Status));

        cook1.start();
        cook2.start();
        cook3.start();

        for (int i = 1; i <= 20; i++) {
            Customer customer = new Customer(i);
            Thread customerThread = new Thread(new CustomerTask(customer, orderQueue, currentCustomersList));
            customerThread.setDaemon(true);
            customerThread.start();
        }

        Button increaseMaxOrderButton = new Button("Increase Max acceptable orders in queue");
        increaseMaxOrderButton.setOnAction(e -> orderQueue.increaseMaxOrders());

        Button decreaseMaxOrderButton = new Button("Decrease Max acceptable orders in queue");
        decreaseMaxOrderButton.setOnAction(e -> orderQueue.decreaseMaxOrders());

        // --- UI Setup ---
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setBackground(new Background(new BackgroundFill(Color.YELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
        ListView<String> customerListView = new ListView<>(currentCustomersList);
        leftPanel.getChildren().addAll(new Label("👤 Customers"), customerListView);

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

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}