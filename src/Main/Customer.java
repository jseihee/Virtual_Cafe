package Main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Customer {
    private final static int port = 8888; //Port number

    // Map of customers (keep track of current customers):
    public static ConcurrentHashMap<String, CustomerStatus> customersMap = new ConcurrentHashMap<>();


    public static String randomName; // A placeholder name for the client-server until name is asked in the ClientHandler


    // Update the key of customersMap to customer name
    public static void updateCustomerName(String oldKey, String newKey){
        // Get the value associated with the old key
        CustomerStatus value = customersMap.remove(oldKey);
        customersMap.put(newKey, value);
    }


    // Get the number of customers currently in the café
    public static int getCustomerCount(){
        return customersMap.size();
    }

    // Changes customer status
    public static void updateCustomerStatus(String customerName, CustomerStatus newStatus) {
        customersMap.put(customerName, newStatus);
    }

    // Removes customer
    public static void removeCustomer(String customerName) {
        customersMap.remove(customerName);
    }

    // Returns the number of customers that are waiting for their order
    public static int countWaitingCustomers() {
        return (int) customersMap.values().stream()
                .filter(status -> status == CustomerStatus.WAITING_FOR_ORDER)
                .count();
    }

    // Enumerate of customer status:
    public enum CustomerStatus {
        // Types of customer status:
        WAITING_FOR_ORDER,
        IDLE
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(port); // Create Server Socket
            System.out.println("Waiting for incoming connections...");
            System.out.println();

            //Keeps listening until exit or program termination
            while (true) {

                Socket clientSocket = serverSocket.accept(); // Connect to client server

                // Put the client socket in the customersMap
                randomName = generateRandomString(5);
                customersMap.put(randomName, CustomerStatus.WAITING_FOR_ORDER);

                System.out.println("New customer connected, " + customersMap.size() + " customers in the café");

                new Thread(new Barista(clientSocket)).start(); // Can handle multiple clients simultaneously
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Generate a random string that will be used as a placeholder until client name is asked in the ClientHandler class
    private static String generateRandomString(int length) {
        // Character range for the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Create a StringBuilder to build the random string
        StringBuilder randomString = new StringBuilder();

        // Create a Random object
        Random random = new Random();

        // Generate the random string
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            char randomChar = characters.charAt(index);
            randomString.append(randomChar);
        }
        return randomString.toString();
    }
}
