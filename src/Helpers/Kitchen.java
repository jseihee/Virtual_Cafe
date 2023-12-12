package Helpers;

import Main.Customer;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

public class Kitchen {
    PrintWriter writer;
    Order currentOrder;

    // Areas of the café:
        public Queue<String> waitingArea = new LinkedBlockingQueue<>();
        public Queue<String> waitingAreaCopy = new LinkedBlockingQueue<>();

        public Queue<String> brewingArea = new LinkedBlockingQueue<>();
        public Queue<String> trayArea = new LinkedBlockingQueue<>();


    // Limit the number of coffees & teas brewed simultaneously
        private final int maxCoffeeCount = 2; private final int maxTeaCount = 2;
        // Create separate executor services for coffee and tea
        ExecutorService coffeeExecutor = Executors.newFixedThreadPool(maxCoffeeCount);
        ExecutorService teaExecutor = Executors.newFixedThreadPool(maxTeaCount);


    //Other variables:
        String coffeeEmoji = "\u2615";
        String teaEmoji = "🍵";
        // Used to signal if customer order is ready
        public final CountDownLatch brewingLatch = new CountDownLatch(1);

        // Used to debug
        int waitingAreaCount;


        // Keeps track of customers that left the café
        public static Map<String, Queue<String>> previousCustomers = new ConcurrentHashMap<>();



    public Kitchen(PrintWriter writer) {
        this.writer = writer;
    }

    // Accepts customer order
    public void acceptOrder(Order order) {
        currentOrder = order;

        // Move beverages of this order to <waiting area>
        waitingArea.addAll(Collections.nCopies(order.getTeaCount(), "tea"));
        waitingArea.addAll(Collections.nCopies(order.getCoffeeCount(), "coffee"));
        waitingAreaCopy = new LinkedBlockingQueue<>(waitingArea);
        waitingAreaCount = waitingArea.size();

        // Message the client that order has been received
        writer.println("");
        writer.println("[Order received for " + order.customerName + " for "
                + order.getTeaCount() + " tea(s) and " + order.getCoffeeCount() + " coffees]");
        writer.println("");

        // Start brewing this order
        brewingTask();
    }

    // Starts the task of brewing the beverage
    private void brewingTask(){
        // Loops until waiting area is empty
        while (waitingAreaCount!=0) {
            waitingAreaCount--;

            String beverage = waitingArea.poll();

            // Submit tea tasks
            if (Objects.equals(beverage, "tea")){
                teaExecutor.submit(() -> brewingProcess("tea"));
            }
            // Submit coffee tasks
            else if (Objects.equals(beverage, "coffee")){
                coffeeExecutor.submit(() -> brewingProcess("coffee"));
            }
        }

        // Wait until all the brewing (full order) is finished
        try {
            brewingLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Message the client that their order is ready for collection
        writer.println("");
        writer.println(currentOrder.customerName + ", your order of " + currentOrder.getTeaCount() +
                " tea(s) and " + currentOrder.getCoffeeCount() + " coffee(s) is ready. Enjoy!");
        writer.println("");

        // Remove this order from the tray area:
        trayArea.clear();

        // Make customer status idle:
        Customer.updateCustomerStatus(currentOrder.customerName, Customer.CustomerStatus.IDLE);
        System.out.println(currentOrder.customerName + "'s order finished, " +
                Customer.countWaitingCustomers() + " customers waiting ");
    }


    //Brewing process:
    private void brewingProcess(String currentBeverage) {

        waitingAreaCopy.remove(currentBeverage);
        brewingArea.offer(currentBeverage);

        switch (currentBeverage) {
            case "tea":
                try {
//                    writer.println("Tea is brewing..");
                    Thread.sleep(4000); // 30-sec brewing time for tea 30000
                    writer.println("Tea is ready " + teaEmoji);

                    currentOrder.setTeaReady(true);
                    completeBrewing(currentBeverage);

                    System.out.println(countOccurrences(waitingAreaCopy, "tea") + " teas and " + countOccurrences(waitingAreaCopy, "coffee") + " coffees in the waiting area, "
                            + countOccurrences(brewingArea, "tea") + " teas and " + countOccurrences(brewingArea, "coffee") + " coffees in the brewing area, "
                            + countOccurrences(trayArea, "tea") + " teas and " + countOccurrences(trayArea, "coffee") + " coffees in the tray area. ");

                    if (currentOrder.teaReady == currentOrder.getTeaCount() && currentOrder.coffeeReady == currentOrder.getCoffeeCount()) {
                        // Signal that  the full order is finished
                        brewingLatch.countDown();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;

            case "coffee":
                try {
//                    writer.println("Coffee is brewing..");
                    Thread.sleep(1000); // 45-sec brewing time for coffee 45000
                    writer.println("Coffee is ready " + coffeeEmoji);

                    currentOrder.setCoffeeReady(true);
                    completeBrewing(currentBeverage);

                    System.out.println(countOccurrences(waitingAreaCopy, "tea") + " teas and " + countOccurrences(waitingAreaCopy, "coffee") + " coffees in the waiting area, "
                            + countOccurrences(brewingArea, "tea") + " teas and " + countOccurrences(brewingArea, "coffee") + " coffees in the brewing area, "
                            + countOccurrences(trayArea, "tea") + " teas and " + countOccurrences(trayArea, "coffee") + " coffees in the tray area. ");

                    if (currentOrder.teaReady == currentOrder.getTeaCount() && currentOrder.coffeeReady == currentOrder.getCoffeeCount()) {
                        // Signal that  the full order is finished
                        brewingLatch.countDown();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    private void completeBrewing(String currentBeverage) {
        // Move the beverage to the tray area after brewing
        brewingArea.remove(currentBeverage);
        trayArea.offer(currentBeverage);
    }

    public void addToOrderAndBrew(int teaCount, int coffeeCount){
        waitingArea.addAll(Collections.nCopies(teaCount, "tea"));
        waitingArea.addAll(Collections.nCopies(coffeeCount, "coffee"));
        waitingAreaCount = waitingAreaCount + teaCount + coffeeCount;

        currentOrder.updateTeaCount(teaCount);
        currentOrder.updateCoffeeCount(coffeeCount);

        for (int i = 1; i <= teaCount; i++) {
            waitingAreaCount--;
            teaExecutor.submit(() -> brewingProcess("tea"));
        }
        for (int i = 1; i <= coffeeCount; i++) {
            waitingAreaCount--;
            coffeeExecutor.submit(() -> brewingProcess("coffee"));
        }
    }

    // Order status
    public void getOrderStatus(Order order){
        if (waitingArea.isEmpty() && brewingArea.isEmpty() && trayArea.isEmpty()){
            writer.println("No order found for " + order.customerName);
            writer.println("");
        }
        else {
            writer.println("");
            writer.println("Order status for " + order.customerName + ":");

            if (!waitingAreaCopy.isEmpty()){
                // Print orders in the waiting area
                writer.println("- " + countOccurrences(waitingAreaCopy, "tea") + " tea(s) and "
                        + countOccurrences(waitingAreaCopy, "coffee") + " coffee(s) in waiting area");
            }

            if (!brewingArea.isEmpty()){
                // Print orders currently being prepared
                writer.println("- " + countOccurrences(brewingArea, "tea") + " tea(s) and "
                        + countOccurrences(brewingArea, "coffee") + " coffee(s) currently being prepared");
            }

            if (!trayArea.isEmpty()){
                // Print orders in the tray area
                writer.println("- " + countOccurrences(trayArea, "tea") + " tea(s) and "
                        + countOccurrences(trayArea, "coffee") + " coffee(s) in the tray");
            }
            writer.println("");
        }
    }

    public static void updatePreviousCustomers(String customerName, int teaCount, int coffeeCount) {
        Queue<String> initialOrder = new LinkedBlockingQueue<>();
        initialOrder.addAll(Collections.nCopies(teaCount, "tea"));
        initialOrder.addAll(Collections.nCopies(coffeeCount, "coffee"));
        if (previousCustomers.containsKey(customerName)) {
            previousCustomers.replace(customerName, initialOrder);
        } else {
        previousCustomers.put(customerName, initialOrder);
        }
    }



    // Method to count occurrences of an element in a LinkedList
    public static <T> int countOccurrences(Queue<String> area, String beverage) {
        int count = 0;
        for (String element : area) {
            if (element.equals(beverage)) {
                count++;
            }
        }
        return count;
    }


}
