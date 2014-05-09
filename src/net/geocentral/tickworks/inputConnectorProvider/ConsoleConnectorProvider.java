package net.geocentral.tickworks.inputConnectorProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.geocentral.tickworks.InputConnectorProvider;

public class ConsoleConnectorProvider implements InputConnectorProvider<Integer> {
    
    private BlockingQueue<Integer> queue;
    
    public ConsoleConnectorProvider() {
    	queue = new LinkedBlockingQueue<Integer>();
    }
    
    public void start() {
        Runnable runner = new Runnable() {
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Type in an integer for input");
                while (true) {
                    String line = null;
                    Integer inValue;
                    try {
                        line = reader.readLine();
                        if (line == null || line.isEmpty()) {
                            System.out.println("Exiting TickWorks");
                            System.exit(0);
                        }
                        inValue = Integer.parseInt(line);
                        queue.put(inValue);
                    }
                    catch (Exception exception) {
                        String message = String.format("Bad input: %s", line);
                        System.err.println(message);
                    }
                }
            }
        };
        new Thread(runner).start();
    }

    public Integer take() {
        try {
            return queue.take();
        }
        catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
