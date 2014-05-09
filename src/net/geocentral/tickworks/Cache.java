package net.geocentral.tickworks;

import java.util.Iterator;

public interface Cache<T, T1> {

    public void put(T value);

    public Iterator<T> get(T1 query);
    
}
