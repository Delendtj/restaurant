package eksamen.restaurant;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    private static final String[] MEALS = {"Burger", "Fries", "Salad"};
    private static final Random random = new Random();

    public CustomerTask(Customer customer, OrderQueue orderQueue) {
        this.customer = customer;
        this.orderQueue = orderQueue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(3000 + random.nextInt(3000)); // 3–6 sekunder

                String meal = MEALS[random.nextInt(MEALS.length)];
                int prepTime = 1000 + random.nextInt(2000);
                int orderId = getNextOrderId();

                Order order = new Order(orderId, meal, prepTime, customer);
                synchronized (orderQueue) {
                    orderQueue.addOrder(order); // Bruk metoden fra OrderQueue
                }

                System.out.println(customer.getName() + " la inn en ny bestilling: " + meal);
            }
        } catch (InterruptedException e) {
            System.out.println(customer.getName() + " avslutter bestillinger.");
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static int nextOrderId = 6; // Fortsetter etter sample-order #5

    private static synchronized int getNextOrderId() {
        return nextOrderId++;
    }
}

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
    private ObservableList<String> uiOrderList;

    public OrderQueue(int maxOrders, ObservableList<String> uiOrderList) {
    this.maxOrders = maxOrders;
    orders = new LinkedList<>();
    this.uiOrderList = uiOrderList;

        Customer cust1 = new Customer(101, "Alice");
        Customer cust2 = new Customer(102, "Bob");
        Customer cust3 = new Customer(103, "Charlie");
        Customer cust4 = new Customer(104, "Diana");

        // Create some sample Orders (ensure meal types match what your cooks can make)
        Order sampleOrder1 = new Order(1, "Burger", 2000, cust1); // Prep time in ms
        Order sampleOrder2 = new Order(2, "Fries", 1000, cust1);
        Order sampleOrder3 = new Order(3, "Salad", 1500, cust2);
        Order sampleOrder4 = new Order(4, "Burger", 2000, cust3);
        Order sampleOrder5 = new Order(5, "Salad", 1500, cust4);   // Assuming a cook can make "Soda"

        // Add to the queue
        addInitialOrder(sampleOrder1);
        addInitialOrder(sampleOrder2);
        addInitialOrder(sampleOrder3);
        addInitialOrder(sampleOrder4);
        addInitialOrder(sampleOrder5);
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
}

class Cooks implements Runnable {
    private final int cookId;
    private final Set<String> mealtype;
    private final OrderQueue orderQueue;
    private final int prepdelay;
    private final ObservableList<String> servedOrdersList;

    public Cooks(int cookId, Set<String> mealtype, OrderQueue orderQueue, int prepdelay, ObservableList<String> servedOrdersList) {
        this.cookId = cookId;
        this.mealtype = mealtype;
        this.orderQueue = orderQueue;
        this.prepdelay = prepdelay;
        this.servedOrdersList = servedOrdersList;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Order order = orderQueue.getOrder(mealtype);
                if (order != null) {
                    System.out.println("Cook " + cookId + " processing order " + order.getOrderId() + ": " + order.getMealType() + " for Customer " + order.getCustomer().getName());

                    try {
                        long timeToPrepare = order.getPreptime() * prepdelay;
                        Thread.sleep(timeToPrepare);
                        Platform.runLater(() -> servedOrdersList.add("Order " + order.getOrderId() + ": " + order.getMealType() + " for " + order.getCustomer().getName() + " served!"));
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

        // Start cooks in separate threads
        Thread cook1 = new Thread(new Cooks(1, cook1Meals, orderQueue, 10, servedOrdersList));
        Thread cook2 = new Thread(new Cooks(2, cook2Meals, orderQueue, 10, servedOrdersList));
        Thread cook3 = new Thread(new Cooks(3, cook3Meals, orderQueue, 10, servedOrdersList));

        cook1.start();
        cook2.start();
        cook3.start();

        Customer cust1 = new Customer(101, "Alice");
        Customer cust2 = new Customer(102, "Bob");
        Customer cust3 = new Customer(103, "Charlie");
        Customer cust4 = new Customer(104, "Diana");

        new Thread(new CustomerTask(cust1, orderQueue)).start();
        new Thread(new CustomerTask(cust2, orderQueue)).start();
        new Thread(new CustomerTask(cust3, orderQueue)).start();
        new Thread(new CustomerTask(cust4, orderQueue)).start();


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

        // Right - Cooks (static labels for now)
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        rightPanel.getChildren().addAll(
                new Label("⏳ Cook 1 (Burger)"),
                new Label("⏳ Cook 2 (Fries)"),
                new Label("⏳ Cook 3 (Salad)")
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
