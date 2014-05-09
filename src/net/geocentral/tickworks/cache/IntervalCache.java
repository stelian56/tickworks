package net.geocentral.tickworks.cache;

import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import net.geocentral.tickworks.Cache;

public class IntervalCache<T extends Comparable<T>> implements Cache<T, Interval<T>> {

    private NavigableSet<T> cache;
    private Iterator<T> emptyIterator;
    
    public IntervalCache() {
        cache = new ConcurrentSkipListSet<T>();
        Set<T> emptySet = Collections.emptySet();
        emptyIterator = emptySet.iterator();
    }

    public Iterator<T> get(Interval<T> interval) {
        T left = interval.left;
        T right = interval.right;
        boolean leftInclusive = interval.leftInclusive;
        boolean rightInclusive = interval.rightInclusive;
        if (left != null) {
            if (right != null) {
                if (left.compareTo(right) <= 0) {
                    return cache.subSet(left, leftInclusive, right, rightInclusive).iterator();
                }
            }
            else {
                return cache.tailSet(left, leftInclusive).iterator();
            }
        }
        else if (right != null) {
            return cache.headSet(right, rightInclusive).iterator();
        }
        return emptyIterator;
    }
    
    public void put(T value) {
        cache.add(value);
    }
}
