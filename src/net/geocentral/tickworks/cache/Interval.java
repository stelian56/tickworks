package net.geocentral.tickworks.cache;

public class Interval<T> {

    T left;
    T right;
    boolean leftInclusive;
    boolean rightInclusive;
    
    public Interval() {}
    
    public Interval(T left, T right, boolean leftInclusive, boolean rightInclusive) {
        this.left = left;
        this.right = right;
        this.leftInclusive = leftInclusive;
        this.rightInclusive = rightInclusive;
    }
    
    public Interval (T left, T right) {
        this(left, right, true, false);
    }
}
