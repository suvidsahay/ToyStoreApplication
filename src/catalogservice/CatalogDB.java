package catalogservice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CatalogDB {
//    private final String DATABASE_FILE = "/usr/src/app/data/catalog.csv"; // file path to use mounted volume
     private static final String DATABASE_FILE = "catalogservice/data/catalog.csv";
    private final Object fileLock = new Object();

    public static Map<String, CatalogItem> catalog = new ConcurrentHashMap<>();
    public CatalogDB() {
        loadCatalogFromDisk();

        Thread stockCheck = new Thread(new StockCheck());
        stockCheck.start();
    }

    // Load catalog from disk
    private void loadCatalogFromDisk() {
        try (BufferedReader br = new BufferedReader(new FileReader(DATABASE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                String name = parts[0].trim().toLowerCase();
                double price = Double.parseDouble(parts[1].trim());
                int quantity = Integer.parseInt(parts[2].trim());
                catalog.put(name, new CatalogItem(name, price, quantity));
                System.out.println(name + " " + price + " " + quantity);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // Method to get product information based on product name
    public CatalogItem getProduct(String productName) {
        return catalog.get(productName.toLowerCase());
    }

    public void commit() {
        synchronized (fileLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATABASE_FILE))) {
                for(CatalogItem item : catalog.values()) {
                    writer.write(item.getName() + ", " + item.getPrice() + ", " + item.getQuantity() + "\n");
                }
                writer.flush();
                System.out.println("File has been written with lock.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class StockCheck implements Runnable {
        Invalidation invalidation = new Invalidation();
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (CatalogItem item : catalog.values()) {
                    if (item.getQuantity() <= 0) {
                        item.setQuantity(100);
                        catalog.put(item.getName(), item);
                        invalidation.invalidateProduct(item.getName());
                    }
                }
                commit();
                System.out.println("Updated stock values");
            }
        }
    }
}
