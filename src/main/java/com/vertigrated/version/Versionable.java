package com.vertigrated.version;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface Versionable
{
    @Nonnull
    public Version version();
}
