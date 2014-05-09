package net.geocentral.tickworks.function;

import net.geocentral.tickworks.Function2;

public class AddFunction implements Function2<Integer, Integer, Integer> {

    public Integer eval(Integer inValue1, Integer inValue2) {
        Integer outValue = inValue1 + inValue2;
        return outValue;
    }
}
