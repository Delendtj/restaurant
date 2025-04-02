package eksamen.restaurant;

public class Customer extends Thread {
    private static int counter = 1;
    private String customerName;

public Customer() {
    this.customerName = "Kunde " + counter++;
}

public String getCustomerName() {
    return customerName;
}

int orderId;
String mealType;
int preptime;




}


