package net.geocentral.tickworks;

public interface OutputPoint<T> extends ConnectionPoint<T> {

    public void put(Message<T> message);
}
