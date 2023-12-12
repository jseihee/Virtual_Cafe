package Main;

import Helpers.Kitchen;
import Helpers.Order;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Barista implements Runnable{
    private final Socket clientSocket;
    String customerName = "";
    Kitchen coffeeKitchen;
    Order coffeeOrder;
    int ratingSum = 0;

    float avgRating = 0;

    // Executor Service object for managing 'order status' & putting in orders
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    //
    CompletableFuture<Void> liveStatusFuture = CompletableFuture.completedFuture(null);
    ExecutorService liveStatusExecutor = Executors.newSingleThreadExecutor();


    boolean reorder;

    public Barista(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        while (!clientSocket.isClosed()) {
            try (
                    // Read data from customer terminal
                    Scanner scanner = new Scanner(clientSocket.getInputStream());
                    // Send & write data to customer terminal
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

                try {
                    // Welcome customer and take their name
                    writer.println("");
                    writer.println("Hi! Welcome to Cafénergize!");
                    writer.println("May I take your name?");
                    customerName = scanner.nextLine();

                    // Update customerName in the customersMap of Customer class
                    Customer.updateCustomerName(Customer.randomName, customerName);


                    // Revisiting customer
                    if (Kitchen.previousCustomers.containsKey(customerName)){
                        writer.println();
                        writer.println("Welcome back " + customerName + "!");
                        System.out.println("Previous customers map" + Kitchen.previousCustomers);
                        writer.println("Would you like to re-order your previous order with us of "
                                + Kitchen.countOccurrences(Kitchen.previousCustomers.get(customerName), "tea")
                                + " tea(s) and " + Kitchen.countOccurrences(Kitchen.previousCustomers.get(customerName), "coffee")
                                + " coffee(s)?");
                    }

                    else{
                        writer.println("Thank you, " + customerName + ".");
                        writer.println("");

                        // Take order from customer
                        writer.println("This is our menu:");
                        Menu(writer);
                        writer.println("What would you like to order?");
                    }

                    // Continue processing while there continues to be customer input
                    while (true) {
                        String line = scanner.nextLine(); // input


                        // Command 1 ) Customer leaves café with 'exit' command
                        if ("exit".equalsIgnoreCase(line)) {
                            Customer.removeCustomer(customerName); // Remove customer from customers map
                            writer.println("Thanks for visiting!");
                            writer.println("Please take a moment to rate our service from 1-5");

                            // Add leaving customer to the map of previous customers
                            Kitchen.updatePreviousCustomers(customerName, coffeeOrder.getTeaCount(), coffeeOrder.getCoffeeCount());

                            if (scanner.hasNext()){
                                int rating = scanner.nextInt(); // input
                                ratingSum = ratingSum + rating;
                                avgRating = ratingSum/Kitchen.previousCustomers.size();
                            }

                            System.out.println(customerName + " left the café ("
                                    + Customer.customersMap.size() + " remaining customers)");
                            System.out.println("Average rating of service is " + avgRating);

                            break;
                        }

                        // Command 2) Customer receives live order status with "order status" command
                        else if ("order status".equalsIgnoreCase(line)){
                            // Start a thread for live order status updates
                            liveStatusFuture = CompletableFuture.runAsync(() -> coffeeKitchen.getOrderStatus(coffeeOrder), liveStatusExecutor);
                        }

                        // Command 3) Customer orders coffee and/or tea
                        else if (getBeverageCount(line)[0]!=0 || getBeverageCount(line)[1]!=0 || "yes".equalsIgnoreCase(line)){
                            // Start a thread for processing user commands
                            CompletableFuture.runAsync(() -> {
                                int[] counts = getBeverageCount(line);
                                int teaCount;
                                int coffeeCount;

                                if ("yes".equalsIgnoreCase(line)){
                                    teaCount = Kitchen.countOccurrences(Kitchen.previousCustomers.get(customerName), "tea");
                                    coffeeCount = Kitchen.countOccurrences(Kitchen.previousCustomers.get(customerName), "coffee");
                                    reorder=false;
                                }
                                else{
                                    teaCount = counts[0];
                                    coffeeCount = counts[1];
                                }

                                // Place a valid order
                                if (teaCount != 0 || coffeeCount != 0) {
                                    // Add to order if current customer order is not finished
                                    if (coffeeKitchen!=null && coffeeOrder!=null && coffeeKitchen.brewingLatch.getCount()!=0){
                                        coffeeKitchen.addToOrderAndBrew(teaCount, coffeeCount);
                                    }
                                    // Start a new order
                                    else{
                                        coffeeOrder = new Order(teaCount, coffeeCount, customerName);
                                        coffeeKitchen = new Kitchen(writer);
                                        coffeeKitchen.acceptOrder(coffeeOrder);
                                    }
                                }}, executorService);
                        }

                        // Handle unknown command
                        else{
                            writer.println("Sorry, I can't understand you");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    // Get the number of teas and coffees of a customers order from terminal input
    private int[] getBeverageCount(String line){
        String[] substrings = line.split(" ");
        int coffeeCount = 0;
        int teaCount = 0;

        // Filtering out customer's order
        for (int i = 0; i < substrings.length; i++) {
            if ("coffee".equalsIgnoreCase(substrings[i]) || "coffees".equalsIgnoreCase(substrings[i])) {
                if (i > 0 && isInteger(substrings[i - 1])) {
                    coffeeCount = Integer.parseInt(substrings[i - 1]);
                }
            } else if ("tea".equalsIgnoreCase(substrings[i]) || "teas".equalsIgnoreCase(substrings[i])) {
                if (i > 0 && isInteger(substrings[i - 1])) {
                    teaCount = Integer.parseInt(substrings[i - 1]);
                }
            }
        }
        return new int[]{teaCount, coffeeCount};
    }

    // For printing the menu
    public void Menu(PrintWriter writer){
        // Menu items
        String coffee = "Coffee";
        String tea = "Tea";

        // Display the menu box
        printMenuBox(writer);

        // Display menu items
        writer.println("| " + coffee);
        writer.println("| " + tea);

        // Display the bottom of the menu box
        printMenuBox(writer);
    }

    // Helper method to print the menu box
    private static void printMenuBox(PrintWriter writer) {
        int boxWidth = 25;
        writer.println("+" + "-".repeat(boxWidth - 2) + "+");
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
