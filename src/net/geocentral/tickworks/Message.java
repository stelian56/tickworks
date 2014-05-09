package net.geocentral.tickworks;

public class Message<T> {

    public T value;
    
    public Message(T value) {
        this.value = value;
    }
}
