package net.geocentral.tickworks;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MyParameterizedType implements ParameterizedType {

    private MyRawType rawType;
    private Type[] typeParameters;

    public MyParameterizedType(MyRawType rawType, Type[] typeParameters) {
        this.rawType = rawType;
        this.typeParameters = typeParameters;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(rawType);
        buf.append("<");
        int parameterCount = typeParameters.length;
        for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
            Type typeParameter = typeParameters[parameterIndex];
            buf.append(typeParameter);
            if (parameterIndex < parameterCount - 1) {
                buf.append(", ");
            }
            buf.append(">");
        }
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType type = (ParameterizedType)obj;
        if (!rawType.equals(type.getRawType())) {
            return false;
        }
        int parameterCount = typeParameters.length;
        Type[] typeArguments = type.getActualTypeArguments();
        if (typeArguments.length != parameterCount) {
            return false;
        }
        for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
            Type typeParameter = typeArguments[parameterIndex];
            if (!typeParameters[parameterIndex].equals(typeParameter)) {
                return false;
            }
        }
        return true;
    }

    public Type getRawType() {
        return rawType;
    }

    public Type[] getActualTypeArguments() {
        return typeParameters;
    }

    public Type getOwnerType() {
        return null;
    }
}
