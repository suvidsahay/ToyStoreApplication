package frontendservice.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * This is a singleton class which manages the cache for catalogs. It uses LinkedHashMap to implement LRU cache
 * so the least recently queried product would be replaced with new entry in the cache.
 */
public class Cache {
    private Map<String, String> cache;
    int capacity = 15;

    private static Cache instance = null;

    private Cache() {
        cache = Collections.synchronizedMap(new LinkedHashMap<String, String>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Entry<String, String> eldest) {
                return size() > capacity;
            }
        });
    }

    public static synchronized Cache getInstance() {
        if(instance == null) {
            instance = new Cache();
        }
        return instance;
    }


    public void addItem(String key, String value) {
        cache.put(key, value);
    }

    public boolean containsItem(String key) {
        return cache.containsKey(key);
    }

    public String getItem(String key) {
        return cache.get(key);
    }

    public void removeItem(String key) {
        cache.remove(key);
    }

    public void clearCache() {
        cache.clear();
    }
}
