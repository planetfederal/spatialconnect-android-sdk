package com.boundlessgeo.spatialconnect.scutilities;

public class SCTuple<T, U, V> {

    private final T first;
    private final U second;
    private final V third;

    public SCTuple(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public T first() { return first; }
    public U second() { return second; }
    public V third() { return third; }
}
