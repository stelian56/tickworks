package net.geocentral.tickworks.function;

import net.geocentral.tickworks.Function1;
import net.geocentral.tickworks.cache.Interval;

public class IntervalQueryFunction implements Function1<Interval<Integer>, Integer> {

    private static final int spread = 5;
    
    public Interval<Integer> eval(Integer inValue) {
        return new Interval<Integer>(inValue - spread, inValue + spread);
    }

}
