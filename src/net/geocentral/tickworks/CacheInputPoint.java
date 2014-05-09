package net.geocentral.tickworks;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CacheInputPoint<T> implements OutputPoint<T> {

    private String id;
    private ExecutorService executor;
    private long timeout;
    private TimeUnit timeUnit;
    private Cache<T, ?> cache;
    
    public CacheInputPoint(String id, ExecutorService executor, long timeout, TimeUnit timeUnit) {
        this.id = id;
        this.executor = executor;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public void setCache(Cache<T, ?> cache) {
        this.cache = cache;
    }

    public void put(final Message<T> message) {
        Future<Boolean> future = executor.submit(new Callable<Boolean>() {
            public Boolean call() {
                T value = message.value;
                if (value != null) {
                    cache.put(value);
                }
                return true;
            };
        });
        try {
            future.get(timeout, timeUnit);
        }
        catch (TimeoutException exception) {
            String errorMessage = String.format("Cache put execution at connection point '%s' timed out after %d %s",
                    id, timeout, timeUnit);
            System.err.println(errorMessage);
            future.cancel(true);
        }
        catch (Exception exception) {
            String errorMessage = String.format("Cache put execution error at connection point '%s': %s",
                    id, exception.getMessage());
            System.err.println(errorMessage);
        }
    }
}
