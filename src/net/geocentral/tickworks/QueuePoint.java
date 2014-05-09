package net.geocentral.tickworks;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueuePoint<T> implements OutputPoint<T> {

    private String id;
    private BlockingQueue<Message<T>> queue;
    
    public QueuePoint(String id) {
        this.id = id;
        queue = new LinkedBlockingQueue<Message<T>>();
    }

    public Message<T> take() {
        try {
            return queue.take();
        }
        catch (InterruptedException exception) {
            String errorMessage =
                    String.format("InterruptedException at connection point '%s' take. Exiting TickWorks", id);
            System.err.println(errorMessage);
            System.exit(1);
            return null;
        }
    }

    public void put(Message<T> message) {
        try {
            queue.put(message);
        }
        catch (InterruptedException exception) {
            String errorMessage =
                    String.format("InterruptedException at connection point '%s' put. Exiting TickWorks", id);
            System.err.println(errorMessage);
            System.exit(1);
        }
    }
}
