package net.geocentral.tickworks.function;

import net.geocentral.tickworks.Function1;

public class CubeFunction implements Function1<Integer, Integer> {

    public Integer eval(Integer inValue) {
        Integer outValue = inValue * inValue * inValue;
        return outValue;
    }
}
