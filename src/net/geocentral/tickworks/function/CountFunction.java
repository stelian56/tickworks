package net.geocentral.tickworks.function;

import java.util.Iterator;

import net.geocentral.tickworks.Function2;

public class CountFunction<T> implements Function2<Integer, T, Iterator<T>> {

    public Integer eval(T inValue1, Iterator<T> inValue2) {
        int count = 0;
        while (inValue2.hasNext()) {
            count++;
            inValue2.next();
        }
        return count;
    }
}
