package net.geocentral.tickworks.generated;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.geocentral.tickworks.CacheOutputPoint;
import net.geocentral.tickworks.Message;
import net.geocentral.tickworks.OutputPoint;
import net.geocentral.tickworks.QueuePoint;
import net.geocentral.tickworks.Rule;

public class IntervalCountRule implements Rule {

    private ExecutorService executor;
    private long timeout;
    private TimeUnit timeUnit;
    private QueuePoint<java.lang.Integer> inputPoint1;
    private CacheOutputPoint<java.lang.Integer, net.geocentral.tickworks.cache.Interval<java.lang.Integer>> inputPoint2;
    private net.geocentral.tickworks.Function1<net.geocentral.tickworks.cache.Interval<java.lang.Integer>, java.lang.Integer> queryFunction2;
    private List<OutputPoint<java.lang.Integer>> outputPoints;
    private net.geocentral.tickworks.Function2<java.lang.Integer, java.lang.Integer, Iterator<java.lang.Integer>> function;

    public IntervalCountRule(ExecutorService executor, long timeout, TimeUnit timeUnit) {
        this.executor = executor;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        outputPoints = new ArrayList<OutputPoint<java.lang.Integer>>();
    }

    public void setInputPoint1(QueuePoint<java.lang.Integer> inputPoint1) {
        this.inputPoint1 = inputPoint1;
    }

    public void setInputPoint2(CacheOutputPoint<java.lang.Integer, net.geocentral.tickworks.cache.Interval<java.lang.Integer>> inputPoint2) {
        this.inputPoint2 = inputPoint2;
    }

    public void addOutputPoint(OutputPoint<java.lang.Integer> outputPoint) {
        outputPoints.add(outputPoint);
    }

    public void setFunction(net.geocentral.tickworks.Function2<java.lang.Integer, java.lang.Integer, Iterator<java.lang.Integer>> function) {
        this.function = function;
    }

    public void setQueryFunction2(net.geocentral.tickworks.Function1<net.geocentral.tickworks.cache.Interval<java.lang.Integer>, java.lang.Integer> queryFunction2) {
        this.queryFunction2 = queryFunction2;
    }

    public void start() {
        Runnable runner = new Runnable() {
            public void run() {
                while (true) {
                    java.lang.Integer outValue = null;
                    Message<java.lang.Integer> inMessage1;
                    inMessage1 = inputPoint1.take();
                    final java.lang.Integer inValue1 = inMessage1.value;
                    if (inValue1 != null) {
                        net.geocentral.tickworks.cache.Interval<java.lang.Integer> query2 = null;
                        Future<net.geocentral.tickworks.cache.Interval<java.lang.Integer>> future2 = executor.submit(new Callable<net.geocentral.tickworks.cache.Interval<java.lang.Integer>>() {
                            public net.geocentral.tickworks.cache.Interval<java.lang.Integer> call() {
                                return queryFunction2.eval(inValue1);
                            };
                        });
                        try {
                            query2 = future2.get(timeout, timeUnit);
                        }
                        catch (TimeoutException exception) {
                            String message = String.format("Query function 'IntervalQueryFunction' timed out after %d %s", timeout, timeUnit);
                            System.err.println(message);
                            future2.cancel(true);
                        }
                        catch (Exception exception) {
                            String message = String.format("Query function 'IntervalQueryFunction' evaluation error: %s", exception.getMessage());
                            System.err.println(message);
                        }
                        if (query2 != null) {
                            final Iterator<java.lang.Integer> inValue2 = inputPoint2.get(query2);
                            if (inValue2 != null) {
                                Future<java.lang.Integer> future = executor.submit(new Callable<java.lang.Integer>() {
                                    public java.lang.Integer call() {
                                        return function.eval(inValue1, inValue2);
                                    };
                                });
                                try {
                                    outValue = future.get(timeout, timeUnit);
                                }
                                catch (TimeoutException exception) {
                                    String message = String.format("Function 'CountFunction' timed out after %d %s", timeout, timeUnit);
                                    System.err.println(message);
                                    future.cancel(true);
                                }
                                catch (Exception exception) {
                                    String message = String.format("Function 'CountFunction' evaluation error: %s", exception.getMessage());
                                    System.err.println(message);
                                }
                            }
                        }
                    }
                    Message<java.lang.Integer> outMessage = new Message<java.lang.Integer>(outValue);
                    for (OutputPoint<java.lang.Integer> outputPoint : outputPoints) {
                        outputPoint.put(outMessage);
                    }
                }
            }
        };
        new Thread(runner).start();
    }
}
