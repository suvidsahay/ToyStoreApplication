package client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Client {
    public static void main(String[] args) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            List<String> products = new ArrayList<>(
                    Arrays.asList("mrpotatohead", "dolphin", "python", "chess", "skateboard", "bicycle", "rollerskates",
                            "barbie", "frisbee", "fox", "rubikscube", "yo-yo", "dominoes", "lego", "whale", "jumprope",
                            "tux", "monopoly", "uno", "bingo", "hotwheels", "elephant")
            );
            String remoteHost = System.getenv("REMOTE_HOST");

            List<Order> orders = new ArrayList<>();

            for(int i = 0; i < 20; i++) {
                Random random = new Random();
                int index = random.nextInt(products.size());
                String randomProduct = products.get(index);

                // Perform a GET request
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(new URI("http://" + remoteHost + "/products/" + randomProduct))
                        .GET() // This is the default and can be omitted
                        .build();

                HttpResponse<String> getResponse = client.send(getRequest, BodyHandlers.ofString());
                System.out.println("GET Response: " + getResponse.body());

                double randomDouble = random.nextDouble();
                double p = 0.7;

                if(randomDouble <= p && getResponse.statusCode() == 200) {
                    // Perform a POST request
                    HttpRequest postRequest = HttpRequest.newBuilder()
                            .uri(new URI("http://" + remoteHost + "/order"))
                            .header("Content-Type", "application/json")
                            .POST(BodyPublishers.ofString(String.format("{\"name\":\"%s\", \"quantity\":%s}",
                                    randomProduct, 1)))
                            .build();

                    HttpResponse<String> postResponse = client.send(postRequest, BodyHandlers.ofString());
                    System.out.println("POST Response: " + postResponse.body());
                    if(postResponse.statusCode() == 200) {
                        orders.add(new Order(getOrderNumber(postResponse.body().replaceAll("[ \t\n]+", "")),
                                randomProduct, 1));
                    }
                }
            }

            for(Order order : orders) {
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(new URI("http://" + remoteHost + "/order/" + order.orderNumber))
                        .GET() // This is the default and can be omitted
                        .build();

                HttpResponse<String> getResponse = client.send(getRequest, BodyHandlers.ofString());
                Order returnedOrder = new Order(getOrderNumberFromGetRequest(getResponse.body().replaceAll("[ \t\n]+", "")),
                        getProductName(getResponse.body().replaceAll("[ \t\n]+", "")),
                        getQuantity(getResponse.body().replaceAll("[ \t\n]+", "")));

                if(!returnedOrder.equals(order)) {
                    System.out.println("Returned order " + returnedOrder + " not equal to given order: " + order);
                }
            }
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static int getQuantity(String body) {
        int startIndex = body.indexOf("quantity");
        return Integer.parseInt(body.substring(body.indexOf(":", startIndex) + 1,
                body.indexOf("}", startIndex)));
    }

    private static String getOrderNumberFromGetRequest(String body) {
        int startIndex = body.indexOf("number");
        return body.substring(body.indexOf(":", startIndex) + 2, body.indexOf(",", startIndex) - 1);
    }

    private static String getProductName(String body) {
        int startIndex = body.indexOf("name");
        return body.substring(body.indexOf(":", startIndex) + 2, body.indexOf(",", startIndex) - 1);
    }

    private static String getOrderNumber(String requestBody) {
        int startIndex = requestBody.indexOf("order_number");
        return requestBody.substring(requestBody.indexOf(":", startIndex) + 2, requestBody.indexOf("}", startIndex) - 1);
    }

    public static class Order {
        public String orderNumber;
        public String productName;
        public int quantity;

        public Order(String orderNumber, String productName, int quantity) {
            this.orderNumber = orderNumber;
            this.productName = productName;
            this.quantity = quantity;
        }

        @Override
        public boolean equals(Object object) {
            Order order = (Order)object;
            return this.orderNumber.equals(order.orderNumber) &&
                    this.productName.equals(order.productName) &&
                    this.quantity == order.quantity;
        }

        @Override
        public String toString() {
            return "Order{" +
                    "orderNumber='" + orderNumber + '\'' +
                    ", productName='" + productName + '\'' +
                    ", quantity=" + quantity +
                    '}';
        }
    }
}
