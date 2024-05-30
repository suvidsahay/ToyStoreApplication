package frontendservice.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Any requests that come to /order will be handled in this handler.
 *
 * The OrderHandler handles 2 kind of requests:
 *
 * 1. GET /order/<order_number>: This will return the order details for the given order number.
 * It will either return a 404 response if the order is not available in the logs.
 * It will return a 200 response and order details if there is an order in the logs.
 *
 * 2. POST /order: This method will read the JSON body and send the buy request to OrderService which will return the order
 * number if the request is successful.
 * It will either return a 404 response if the product is not available or out of service or
 * It will return a 200 response if order was successful.
 *
 */
public class OrderHandler implements HttpHandler {

    String orderHost = null;

    public OrderHandler() {
        System.out.println("Electing Leader:");
        orderHost = getOrderHost();
        if(orderHost == null) {
            throw new RuntimeException("No Order hosts available");
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if("POST".equals(exchange.getRequestMethod())) {
            handlePostRequest(exchange);
        } else if("GET".equals(exchange.getRequestMethod())) {
            handleGetRequest(exchange);
        } else {
            exchange.sendResponseHeaders(404, 0);
        }
    }

    private void handleGetRequest(HttpExchange exchange) {
        try (Socket socket = new Socket()) {
            SocketAddress socketAddress = new InetSocketAddress(orderHost, 8086);
            socket.connect(socketAddress, 1000);
            // writing to server

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // reading from server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String path = exchange.getRequestURI().getPath();
            String order = path.substring(path.lastIndexOf('/') + 1);
            System.out.println("Querying order: " + order);

            out.println("QUERY," + order);
            out.flush();

            // displaying server reply
            String reply = in.readLine();
            System.out.println("Server replied: " + reply);
            OutputStream os = exchange.getResponseBody();

            if (reply.equals("-1")) {
                String response = buildQueryErrorResponse();
                exchange.sendResponseHeaders(404, response.length());
                os.write(response.getBytes());
            } else {
                String response = buildQuerySuccessResponse(reply);
                exchange.sendResponseHeaders(200, response.length());
                os.write(response.getBytes());
            }
            os.close();
        } catch (IOException e) {
            System.out.println("LEADER IS DEAD!!!. Starting re-election");
            orderHost = getOrderHost();
            if(orderHost == null) {
                throw new RuntimeException("No Order hosts available");
            }
            handleGetRequest(exchange);
        }
    }

    private void handlePostRequest(HttpExchange exchange) {
        try (Socket socket = new Socket()) {
            SocketAddress socketAddress = new InetSocketAddress(orderHost, 8086);
            socket.connect(socketAddress, 1000);
            // writing to server

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // reading from server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String requestBody = getRequestBody(exchange);
            System.out.println(requestBody);

            String product = getProduct(requestBody);
            int quantity = getQuantity(requestBody);
            System.out.println(product);
            System.out.println(quantity);

            out.println("BUY," + product + "," + quantity);
            // sending the user input to server
            out.flush();

            // displaying server reply
            String reply = in.readLine();
            System.out.println("Server replied: " + reply);
            OutputStream os = exchange.getResponseBody();

            String response;
            if (reply.equals("-1")) {
                response = buildErrorResponse();
                exchange.sendResponseHeaders(404, response.length());
            } else {
                response = buildSuccessResponse(reply);
                exchange.sendResponseHeaders(200, response.length());
            }
            os.write(response.getBytes());
            os.close();

        } catch (IOException e) {
            System.out.println("LEADER IS DEAD!!!. Starting re-election");
            orderHost = getOrderHost();
            if(orderHost == null) {
                throw new RuntimeException("No Order hosts available");
            }
            handlePostRequest(exchange);
        }
    }

    private String buildQuerySuccessResponse(String reply) {
        String orderNumber = reply.split(",")[0];
        String product = reply.split(",")[1];
        String quantity = reply.split(",")[2];

        return String.format("{\n\t\"data\": {\n\t\t\"number\": \"%s\",\n\t\t\"name\": \"%s\",\n\t\t\"quantity\": %s\n\t}\n}",
                orderNumber, product, quantity);
    }

    private String buildQueryErrorResponse() {
        return "{\n\t\"error\": {\n\t\t\"code\": 404,\n\t\t\"message\": \"order not found\"\n\t}\n}";
    }

    private String buildSuccessResponse(String reply) {
        String orderNumber = reply;

        return String.format("{\n\t\"data\": {\n\t\t\"order_number\": \"%s\"\n\t}\n}", orderNumber);
    }

    private String buildErrorResponse() {
        return "{\n\t\"error\": {\n\t\t\"code\": 404,\n\t\t\"message\": \"product not found or out of stock\"\n\t}\n}";
    }

    private String getProduct(String requestBody) {
        int startIndex = requestBody.indexOf("name");
        return requestBody.substring(requestBody.indexOf(":", startIndex) + 2, requestBody.indexOf(",", startIndex) - 1);
    }

    private int getQuantity(String requestBody) {
        int startIndex = requestBody.indexOf("quantity");
        return Integer.parseInt(requestBody.substring(requestBody.indexOf(":", startIndex) + 1,
                requestBody.indexOf("}", startIndex)));
    }

    private String getRequestBody(HttpExchange exchange) {
        try {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);

            // Use a StringBuilder to collect lines
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            // Close the BufferedReader and InputStreamReader
            br.close();
            return requestBody.toString().replaceAll("[ \t]+", "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getOrderHost() {
        List<Entry<Integer, String>> nodes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("frontendservice/handler/nodes.properties"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                Entry<Integer, String> entry = new SimpleEntry<>(Integer.parseInt(parts[0]), parts[1]);
                nodes.add(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Collections.sort(nodes, (n1, n2) -> n2.getKey() - n1.getKey());

        for(Entry<Integer, String> entry : nodes) {
            if(isHealthy(entry.getValue())) {
                System.out.println("Selected node with address: " + entry.getValue());
                notifyLeader(entry.getValue());
                return entry.getValue();
            } else {
                System.out.println("Node is not alive: " + entry.getValue());
            }
        }
        return null;
    }

    private boolean isHealthy(String orderHost) {
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
            e.printStackTrace();
            return false;
        }
    }

    private void notifyLeader(String orderHost) {
        try (Socket socket = new Socket(orderHost, 8086)) {
            // writing to server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // reading from server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LEADER");
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
