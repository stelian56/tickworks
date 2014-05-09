package net.geocentral.tickworks.function;

import net.geocentral.tickworks.Function1;

public class IdemFunction<T> implements Function1<T, T> {

    public T eval(T inValue) {
        return inValue;
    }
}
