package net.geocentral.tickworks.function;

import net.geocentral.tickworks.Function1;

public class SquareFunction implements Function1<Integer, Integer> {

    public Integer eval(Integer inValue) {
        Integer outValue = inValue * inValue;
        return outValue;
    }
}
