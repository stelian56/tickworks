package net.geocentral.tickworks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.geocentral.tickworks.generated.Initializer;

public class Engine {

    private ExecutorService executor;
    private long timeout = 5;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private Processor processor;
    private List<ConnectionPoint<?>> connectionPoints;
    private List<InputConnector<?>> inputConnectors;
    private List<OutputConnector<?>> outputConnectors;

    public Engine() {
        executor = Executors.newCachedThreadPool();
        processor = new Processor();
        connectionPoints = new ArrayList<ConnectionPoint<?>>();
        inputConnectors = new ArrayList<InputConnector<?>>();
        outputConnectors = new ArrayList<OutputConnector<?>>();
    }
    
    public void init() { 
        Initializer initializer = new Initializer();
        initializer.init(connectionPoints, processor, inputConnectors, outputConnectors, executor, timeout, timeUnit);
    }

    public void validate() throws Exception {
    }
    
    public void start() {
        processor.start();
        for (OutputConnector<?> outputConnector : outputConnectors){
            outputConnector.start();
        }
        for (InputConnector<?> inputConnector : inputConnectors){
            inputConnector.start();
        }
    }
    
    public static void main(String[] args) throws Exception {
        Engine engine = new Engine();
        engine.init();
        engine.start();
    }
}
