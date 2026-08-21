package com.ecom.order;

// Order data model - represents one order in our system
public class Order {
    private String orderId;
    private String product;
    private int quantity;
    private double amount;
    private String status;
    private String customerId;

    public Order() {}

    public Order(String orderId, String product, int quantity,
                 double amount, String status, String customerId) {
        this.orderId = orderId;
        this.product = product;
        this.quantity = quantity;
        this.amount = amount;
        this.status = status;
        this.customerId = customerId;
    }

    public String getOrderId()    { return orderId; }
    public String getProduct()    { return product; }
    public int getQuantity()      { return quantity; }
    public double getAmount()     { return amount; }
    public String getStatus()     { return status; }
    public String getCustomerId() { return customerId; }
    public void setStatus(String status) { this.status = status; }
}
