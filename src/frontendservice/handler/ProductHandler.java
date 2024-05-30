package frontendservice.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import frontendservice.cache.Cache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Any requests that come to /products will be handled in this handler.
 *
 * The incoming query requests will be forwarded to Catalog Service and the response will be sent back as JSON back to
 * the client
 */
public class ProductHandler implements HttpHandler {

    Cache cache = Cache.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if("GET".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            String product = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
            System.out.println("Querying product: " + product);



            if(cache.containsItem(product)) {
                System.out.println("Found item in cache: " + product);
                String response = buildSuccessResponse(product, cache.getItem(product));
                exchange.sendResponseHeaders(200, response.length());

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            String catalogHost = System.getenv("CATALOG_HOST");

            try (Socket socket = new Socket(catalogHost, 8085)) {
                // writing to server
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // reading from server
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // sending the user input to server
                out.println("query:" + product);
                out.flush();

                // displaying server reply
                String reply = in.readLine();
                System.out.println("Server replied: " + reply);
                OutputStream os = exchange.getResponseBody();

                if (reply.equals("-1")) {
                    String response = buildErrorResponse();
                    exchange.sendResponseHeaders(404, response.length());
                    os.write(response.getBytes());
                } else {
                    String response = buildSuccessResponse(product, reply);
                    exchange.sendResponseHeaders(200, response.length());
                    os.write(response.getBytes());

                    cache.addItem(product, reply);
                }
                os.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            exchange.sendResponseHeaders(404, 0);
        }
    }

    private String buildSuccessResponse(String product, String reply) {
        String price = reply.split(",")[0];
        String quantity = reply.split(",")[1];

        return String.format("{\n\t\"data\": {\n\t\t\"name\": \"%s\",\n\t\t\"price\": %s,\n\t\t\"quantity\": %s\n\t}\n}",
                product, price, quantity);
    }

    private String buildErrorResponse() {
        return "{\n\t\"error\": {\n\t\t\"code\": 404,\n\t\t\"message\": \"product not found\"\n\t}\n}";
    }
}
