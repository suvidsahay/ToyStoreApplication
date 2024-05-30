package orderservice;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This microservice is used to handle order related logic. It handles the following kind of requests:
 *
 * 1. QUERY, order_number: This will return the order details for the given order number
 *
 * 2. BUY, product_name, quantity: This will perform a buy request and send a request to catalog service for
 * catalog details and quantity updates
 *
 * 3. PING: This request is used as a health check so that other services know that this service is alive
 *
 * 4. LEADER: This request is to assign this order service as the leader so all the order requests are processed in the
 * leader node and the remaining order services receive the updates to write it in their log file.
 *
 * 5. FOLLOW, order_number, product_name, quantity: The leader order service sends this request to all the other order
 * services so the follower services can write the order details in their log.
 *
 * 6. SYNC, order_number: Whenever a new order service is started, it will use this method to sync their logs with other
 * order service available. Once the order_service gets a sync request, it will read the order number and return all
 * the order details line by line since the given order number.
 */
public class OrderService {
     private static final String ORDER_LOG_FILE = "orderservice/logs/order_log.csv";
    private static final Object fileLock = new Object();
    private static boolean isLeader = false;

    private static int nextOrderNumber = 0;

    private static List<String> orderHosts = new ArrayList<>();

    public static void main(String[] args) {
        ServerSocket server = null;
        for(int i = 0; i < args.length; i++) {
            orderHosts.add(args[i]);
        }
        loadOrderNumber();

        syncWithNodes();
        try {
            // server is listening on port 8086
            server = new ServerSocket(8086);
            server.setReuseAddress(true);
            System.out.println("OrderService running on port: 8086");


            // create ThreadPool
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

            // running infinite loop for getting client request
            while (true) {
                // socket object to receive incoming client requests
                Socket client = server.accept();

                // Displaying that new client is connected to server
                System.out.println("New client connected: " + client.getInetAddress());

                // create a new thread object
                ClientHandler clientSock = new ClientHandler(client);

                // Execute the client handler on the executor
                executor.execute(clientSock);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void syncWithNodes() {
        for(int i = 0; i < orderHosts.size(); i++) {
            if(isHealthy(orderHosts.get(i))) {
                updateLogs(orderHosts.get(i));
                return;
            }
        }
    }

    private static void updateLogs(String orderHost) {
        try (Socket socket = new Socket(orderHost, 8086)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // reading from server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("SYNC," + Integer.toString(nextOrderNumber));
            out.flush();

            // displaying server reply
            String reply = in.readLine();
            while(!reply.equals("-1")) {
                System.out.println("Received: " + reply);
                String[] parts = reply.split(",");
                logOrder(Integer.parseInt(parts[0].trim()), parts[1].trim(), Integer.parseInt(parts[2].trim()));
                reply = in.readLine();
                nextOrderNumber++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isHealthy(String orderHost) {
        if(orderHost == null) {
            return false;
        }
        try (Socket socket = new Socket()) {
            SocketAddress socketAddress = new InetSocketAddress(orderHost, 8086);
            socket.connect(socketAddress, 2000);
            // writing to server

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // reading from server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("PING");
            out.flush();

            // displaying server reply
            String reply = in.readLine();

            return reply.equals("ALIVE");
        } catch (IOException e) {
            System.out.println(orderHost + " inactive");
            return false;
        }
    }

    // ClientHandler class
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        // Constructor
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;

            try {
                // get the outputstream of client
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // get the inputstream of client
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split(",");
                    String requestType = parts[0].trim();
                    if(requestType.equals("QUERY")) {
                        String orderDetails = getOrderDetails(parts[1].trim());
                        if(orderDetails == null) {
                            out.println("-1");
                        } else {
                            out.println(orderDetails);
                        }
                    } else if(requestType.equals("BUY")) {
                        // Parse order request and handle accordingly
                        // For simplicity, let's assume line contains the product name and quantity separated by a comma
                        String productName = parts[1].trim();
                        int quantity = Integer.parseInt(parts[2].trim());

                        // Simulate interaction with catalog service to check if the product is in stock
                        if (isInStock(productName, quantity)) {
                            // Generate order number
                            // Log the order
                            logOrder(nextOrderNumber, productName, quantity);
                            // Send order number to the client
                            out.println(nextOrderNumber);
                            if(isLeader) {
                                forwardRequests(nextOrderNumber, productName, quantity);
                            }
                            nextOrderNumber++;
                        } else {
                            // Send failure message to the client
                            out.println("-1");
                        }
                    } else if(requestType.equals("PING")) {
                        out.println("ALIVE");
                    } else if(requestType.equals("LEADER")) {
                        isLeader = true;
                    } else if(requestType.equals("FOLLOW")) {
                        logOrder(Integer.parseInt(parts[1].trim()), parts[2].trim(), Integer.parseInt(parts[3].trim()));
                        nextOrderNumber = Integer.parseInt(parts[1].trim()) + 1;
                    } else if(requestType.equals("SYNC")) {
                        String orderNumber = parts[1].trim();
                        List<String> logs = getLogs(orderNumber);

                        for(String log : logs) {
                            System.out.println(log);
                            out.println(log);
                        }
                        out.println("-1");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void forwardRequests(int orderNumber, String name, int quantity) {
            for(String orderHost : orderHosts) {
                try (Socket socket = new Socket(orderHost, 8086)) {
                    // writing to server

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("FOLLOW," + orderNumber + "," + name + "," + quantity);
                    out.flush();
                } catch (IOException e) {
                    System.out.println("Order host is not alive:" + orderHost);
                }
            }
        }

        private String getOrderDetails(String orderNumber) {
            try (BufferedReader reader = new BufferedReader(new FileReader(ORDER_LOG_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if(parts[0].trim().equals(orderNumber)) {
                        return line;
                    }
                }
                // Extract the order number from the last line and increment by 1
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private List<String> getLogs(String orderNumber) {
            try (BufferedReader reader = new BufferedReader(new FileReader(ORDER_LOG_FILE))) {
                String line;
                List<String> logs = new ArrayList<>();
                boolean found = false;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if(parts[0].trim().equals(orderNumber) || found) {
                        logs.add(line);
                        found = true;
                    }
                }
                // Extract the order number from the last line and increment by 1
                return logs;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean isInStock(String productName, int quantity) {
            String catalogHost = System.getenv("CATALOG_HOST");

            try (Socket socket = new Socket(catalogHost, 8085)) {
                // writing to server
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // reading from server
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // sending the user input to server
                out.println("buy:" + productName + "," + quantity);
                out.flush();

                String reply = in.readLine();
                if(Integer.parseInt(reply) == -1) {
                    return false;
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private static void loadOrderNumber() {
        try (BufferedReader reader = new BufferedReader(new FileReader(ORDER_LOG_FILE))) {
            String line;
            String lastLine = null;
            // Read until the last line is reached
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
            // Extract the order number from the last line and increment by 1
            int latestOrderNumber = 0;
            if (lastLine != null) {
                String[] parts = lastLine.split(",");
                latestOrderNumber = Integer.parseInt(parts[0].trim());
            }
            nextOrderNumber = latestOrderNumber + 1;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to log order to a CSV file
    private static void logOrder(int orderNumber, String productName, int quantity) {
        synchronized (fileLock) {
            // Write the next order number along with the order details to the CSV file
            try (PrintWriter writer = new PrintWriter(new FileWriter(ORDER_LOG_FILE, true))) {
                writer.println(orderNumber + "," + productName + "," + quantity);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
