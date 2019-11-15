package com.vertigrated.identity;

public interface Identifiable<T extends Comparable<T>>
{
    public T identity();
}
