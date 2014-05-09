package net.geocentral.tickworks;

public interface Rule extends ConnectionPointProvider, ConnectionPointConsumer {

    public void start();
}
