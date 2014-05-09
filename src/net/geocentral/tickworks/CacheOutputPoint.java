package net.geocentral.tickworks;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CacheOutputPoint<T, T1> implements ConnectionPoint<T> {

    private String id;
    private ExecutorService executor;
    private long timeout;
    private TimeUnit timeUnit;
    private Cache<T, T1> cache;
    
    public CacheOutputPoint(String id, ExecutorService executor, long timeout, TimeUnit timeUnit) {
        this.id = id;
        this.executor = executor;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public void setCache(Cache<T, T1> cache) {
        this.cache = cache;
    }

    public Iterator<T> get(final T1 query) {
        Future<Iterator<T>> future = executor.submit(new Callable<Iterator<T>>() {
            public Iterator<T> call() {
                return cache.get(query);
            };
        });
        try {
            return future.get(timeout, timeUnit);
        }
        catch (TimeoutException exception) {
            String errorMessage = String.format("Cache get at connection point '%s' timed out after %d %s",
                    id, timeout, timeUnit);
            System.err.println(errorMessage);
            future.cancel(true);
        }
        catch (Exception exception) {
            String errorMessage = String.format("Cache get error at connection point '%s': %s",
                    id, exception.getMessage());
            System.err.println(errorMessage);
        }
        return null;
    }
}
