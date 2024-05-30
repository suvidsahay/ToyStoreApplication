package frontendservice;

import com.sun.net.httpserver.HttpServer;
import frontendservice.handler.InvalidationHandler;
import frontendservice.handler.OrderHandler;
import frontendservice.handler.ProductHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class FrontEndService {
    public static void main(String[] args) {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
            HttpServer httpServerInvalidate = HttpServer.create(new InetSocketAddress(8081), 0);
            httpServer.setExecutor(Executors.newFixedThreadPool(10));
            httpServerInvalidate.setExecutor(Executors.newFixedThreadPool(10));
            httpServer.createContext("/products/", new ProductHandler());
            httpServer.createContext("/order", new OrderHandler());
            httpServerInvalidate.createContext("/invalidate", new InvalidationHandler());

            httpServer.start();
            System.out.println("HttpServer running on port: 8080");

            httpServerInvalidate.start();
            System.out.println("HttpServerInvalidate running on port: 8081");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}