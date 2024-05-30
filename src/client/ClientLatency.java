package client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ClientLatency {
    public static void main(String[] args) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            List<String> products = new ArrayList<>(
                    Arrays.asList("mrpotatohead", "dolphin", "python", "chess", "skateboard", "bicycle", "rollerskates",
                            "barbie", "frisbee", "fox", "rubikscube", "yo-yo", "dominoes", "lego", "whale", "jumprope",
                            "tux", "monopoly", "uno", "bingo", "hotwheels", "elephant")
            );
            String remoteHost = System.getenv("REMOTE_HOST");

            double p = 0.2;


            List<Double> getLatencies = new ArrayList<>();
            List<Double> postLatencies = new ArrayList<>();

            for(int i = 0; i < 20; i++) {
                Random random = new Random();
                int index = random.nextInt(products.size());
                String randomProduct = products.get(index);

                // Perform a GET request
                long startTimeGet = System.currentTimeMillis();
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(new URI("http://" + remoteHost + "/products/" + randomProduct))
                        .GET()
                        .build();

                HttpResponse<String> getResponse = client.send(getRequest, BodyHandlers.ofString());
                long endTimeGet = System.currentTimeMillis();

                getLatencies.add((double)(endTimeGet - startTimeGet));

                // Perform a POST request based on probability
                double randomDouble = random.nextDouble();
                if(randomDouble <= p) {
                    long startTimePost = System.currentTimeMillis();
                    HttpRequest postRequest = HttpRequest.newBuilder()
                            .uri(new URI("http://" + remoteHost + "/order"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"name\":\"%s\", \"quantity\":1}", randomProduct)))
                            .build();

                    HttpResponse<String> postResponse = client.send(postRequest, BodyHandlers.ofString());
                    long endTimePost = System.currentTimeMillis();

                    postLatencies.add((double)(endTimePost - startTimePost));
                }
            }


            System.out.println("Probability: " + p);

            Double average = 0.0;
            for(Double latency : getLatencies) {
                average += latency;
            }
            System.out.println("GET Average: " + average / getLatencies.size());

            average = 0.0;
            for(Double latency : postLatencies) {
                average += latency;
            }
            System.out.println("POST Average: " + average / postLatencies.size());
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}
