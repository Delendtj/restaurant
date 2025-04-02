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