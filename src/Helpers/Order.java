package Helpers;

public class Order {
    private int teaCount;
    private int coffeeCount;
    public int teaReady=0;
    public int coffeeReady=0;
    public String customerName;


    public Order(int teaCount, int coffeeCount, String customerName) {
        this.teaCount = teaCount;
        this.coffeeCount = coffeeCount;
        this.customerName = customerName;
    }



    public int getTeaCount() {
        return teaCount;
    }
    public int getCoffeeCount() {
        return coffeeCount;
    }
    public void setTeaReady(boolean bool) {teaReady++;}
    public void setCoffeeReady(boolean bool) {coffeeReady++;}

    public void updateCoffeeCount(int addCoffee) {
        coffeeCount = coffeeCount + addCoffee;
    }
    public void updateTeaCount(int addTea) {
        teaCount = teaCount + addTea;
    }
}