package net.geocentral.tickworks;

import java.util.ArrayList;
import java.util.List;

public class InputConnector<T> {

    private String id;
    private InputConnectorProvider<T> provider;
    private List<OutputPoint<T>> outputPoints;
    
    public InputConnector(String id) {
        this.id = id;
        outputPoints = new ArrayList<OutputPoint<T>>();
    }
    
    public void setProvider(InputConnectorProvider<T> provider) {
        this.provider = provider;
    }
    
    public void addOutputPoint(OutputPoint<T> outputPoint) {
        outputPoints.add(outputPoint);
    }

    public void start() {
        provider.start();
        Runnable runner = new Runnable() {
            public void run() {
                while (true) {
                    T value = null;
                    try {
                        value = provider.take();
                    }
                    catch (Exception exception) {
                        String errorMessage = String.format(
                                "Exception at input connector '%s' provider: %s. Disabling connector", id, exception);
                        System.err.println(errorMessage);
                        return;
                    }
                    if (value != null) {
                        Message<T> message = new Message<T>(value);
                        for (OutputPoint<T> outputPoint : outputPoints) {
                            outputPoint.put(message);
                        }
                    }
                }
            }
        };
        new Thread(runner).start();
    }
}
