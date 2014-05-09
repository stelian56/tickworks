package net.geocentral.tickworks;

public interface InputConnectorProvider<T> {

    public void start();
    
    public T take();
}
