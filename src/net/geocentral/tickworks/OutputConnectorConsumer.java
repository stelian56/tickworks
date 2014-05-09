package net.geocentral.tickworks;

public interface OutputConnectorConsumer<T> {

    public void start();
    
    public void put(T value);
}
