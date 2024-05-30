package catalogservice;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class Invalidation {
    HttpClient client = HttpClient.newHttpClient();
    String remoteHost = System.getenv("FRONTEND_HOST");

    public void invalidateProduct(String productName) {
        try {
            HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(new URI("http://" + remoteHost + "/invalidate"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(String.format("{\"name\":\"%s\"}", productName)))
                .build();
            HttpResponse<String> postResponse = client.send(postRequest, BodyHandlers.ofString());
            System.out.println("Invalidation request for product sent " + productName);
            if(postResponse.statusCode() != 200) {
                throw new RuntimeException("Expected response code 200 but got: " + postResponse.statusCode());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
