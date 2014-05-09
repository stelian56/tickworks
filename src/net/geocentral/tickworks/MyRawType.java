package net.geocentral.tickworks;

import java.lang.reflect.Type;

public class MyRawType implements Type {

    private Class<?> rawType;

    public MyRawType(Class<?> rawType) {
        this.rawType = rawType;
    }

    public String toString() {
        return rawType.getName();
    }

    public boolean equals(Object obj) {
        if (obj instanceof MyRawType) {
            return ((MyRawType)obj).rawType.equals(rawType);
        }
        if (obj instanceof Class<?>) {
            return ((Class<?>)obj).equals(rawType);
        }
        return false;
    }
}
