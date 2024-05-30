package catalogservice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Catalog Service will receive request from order service or frontend service.
 *
 * It handles 2 kind of requests:
 * 1. query request: This will return the amount and quantity available for the requested item.
 * 2. buy request: This will update the database file catalog.csv with the updated quantity.
 */
public class CatalogService {
    static CatalogDB catalogDB = new CatalogDB();
    static Invalidation invalidation = new Invalidation();

    public static void main(String[] args) {
        ServerSocket server = null;

        try {
            // server is listening on port 8085
            server = new ServerSocket(8085);
            server.setReuseAddress(true);
            System.out.println("CatalogService running on port: 8085");

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
                    // Parse request and handle accordingly
                    System.out.println("Received:" + line);
                    String[] requestTypeAndData = line.split(":");
                    if(requestTypeAndData[0].equals("query")) {
                        CatalogItem item = catalogDB.getProduct(requestTypeAndData[1]);
                        if (item != null && item.getQuantity() != 0) {
                            out.println(item.getPrice() + "," + item.getQuantity());
                        } else {
                            out.println("-1");
                        }
                    } else if(requestTypeAndData[0].equals("buy")) {
                        String[] itemAndQuantity = requestTypeAndData[1].split(",");
                        CatalogItem item = catalogDB.getProduct(itemAndQuantity[0]);
                        int quantity = Integer.parseInt(itemAndQuantity[1]);
                        if (item != null && item.getQuantity() >= quantity) {
                            // Decrement stock in the catalog
                            System.out.println(item.getName() + item.getQuantity());
                            item.setQuantity(item.getQuantity() - quantity);
                            catalogDB.commit();
                            invalidation.invalidateProduct(item.getName());
                            // Send success response
                            out.println(item.getQuantity());
                        } else {
                            // Send failure message to the client
                            out.println("-1");
                        }
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
    }
}
