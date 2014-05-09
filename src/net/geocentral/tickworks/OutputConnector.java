package net.geocentral.tickworks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OutputConnector<T> {

    private String id;
    private ExecutorService executor;
    private long timeout;
    private TimeUnit timeUnit;
    private List<QueuePoint<T>> inputPoints;
    private OutputConnectorConsumer<T> consumer;

    public OutputConnector(String id, ExecutorService executor, long timeout, TimeUnit timeUnit) {
        this.id = id;
        this.executor = executor;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        inputPoints = new ArrayList<QueuePoint<T>>();
    }

    public void setConsumer(OutputConnectorConsumer<T> consumer) {
        this.consumer = consumer;
    }

    public void addInputPoint(QueuePoint<T> inputPoint) {
        inputPoints.add(inputPoint);
    }

    public void start() {
        consumer.start();
        for (final QueuePoint<T> inputPoint : inputPoints) {
            Runnable runner = new Runnable() {
                public void run() {
                    while (true) {
                        Message<T> message = null;
                        message = inputPoint.take();
                        final T value = message.value;
                        if (value != null) {
                            Future<Boolean> future = executor.submit(new Callable<Boolean>() {
                                public Boolean call() {
                                    consumer.put(value);
                                    return true;
                                };
                            });
                            try {
                                future.get(timeout, timeUnit);
                            }
                            catch (TimeoutException exception) {
                                String errorMessage = String.format(
                                    "Output connector '%s' consumer put timed out after %d %s", id, timeout, timeUnit);
                                System.err.println(errorMessage);
                                future.cancel(true);
                            }
                            catch (Exception exception) {
                                String errorMessage = String.format(
                                        "Output connector '%s' consumer execution error: %s",
                                        id, exception.getMessage());
                                System.err.println(errorMessage);
                            }
                        }
                        else {
                            System.err.println(String.format("Ignoring null value at output connector '%s'", id));
                        }
                    }
                }
            };
            new Thread(runner).start();
        }
    }
}
