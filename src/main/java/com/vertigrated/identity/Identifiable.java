package com.vertigrated.identity;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface Identifiable<T extends Comparable<T>>
{
    @Nonnull
    public T identity();
}
