package net.geocentral.tickworks.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.geocentral.tickworks.Cache;

public class PointCache<T> implements Cache<T, T> {

    private Map<T, List<T>> cache;

    public PointCache() {
        cache = new ConcurrentHashMap<T, List<T>>();
    }
    
    public void put(T value) {
        List<T> values = cache.get(value);
        if (values == null) {
            values = new ArrayList<T>();
            cache.put(value, values);
        }
        values.add(value);
    }

    public Iterator<T> get(T query) {
        return cache.get(query).iterator();
    }
}
