package frontendservice.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import frontendservice.cache.Cache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;


/**
 *
 * This handler will handle requests coming to /invalidate endpoint
 *
 * The APIs offered are:
 * 1. POST /invalidate: This method will read the product name from the request body and remove the entry from the cache.
 */
public class InvalidationHandler implements HttpHandler {

    Cache cache = Cache.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if("POST".equals(exchange.getRequestMethod())) {
            String productName = getProduct(getRequestBody(exchange));
            cache.removeItem(productName);

            OutputStream os = exchange.getResponseBody();
            String response = "";
            exchange.sendResponseHeaders(200, response.length());

            os.write(response.getBytes());
            os.close();
        } else {
            exchange.sendResponseHeaders(404, 0);
        }
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

    private String getProduct(String requestBody) {
        int startIndex = requestBody.indexOf("name");
        return requestBody.substring(requestBody.indexOf(":", startIndex) + 2, requestBody.indexOf("}", startIndex) - 1);
    }
}
