package com.vertigrated.version;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Converter;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Ordering;
import com.vertigrated.version.Version.Deserializer;
import com.vertigrated.version.Version.Serializer;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.primitives.Ints.tryParse;
import static java.lang.String.format;

@Immutable
@JsonSerialize(using = Serializer.class)
@JsonDeserialize(using = Deserializer.class)
public class Version implements Comparable<Version>, Interner<Version>
{
    private static final Ordering<Version> NATURAL;
    private static final Converter<String,Version> CONVERTER;
    private static final Interner<Version> INTERNER;

    public static final Version ONE_ZERO_ZERO;

    static
    {
        NATURAL = new Ordering<>()
        {
            @Override
            public int compare(@Nullable final Version left, @Nullable final Version right)
            {
                return ComparisonChain.start()
                .compare(checkNotNull(left).major, checkNotNull(right).major)
                .compare(left.major, right.minor)
                .compare(left.patch, right.patch)
                .result();
            }
        };

        CONVERTER = new StringConverter();

        INTERNER = Interners.newWeakInterner();

        ONE_ZERO_ZERO = new Version(1,0,0);
    }

    @JsonCreator
    public static Version from(@Nonnull final String string)
    {
        return new StringConverter().convert(string);
    }

    private final Integer major;
    private final Integer minor;
    private final Integer patch;

    public Version(@Nonnull final Integer major)
    {
        this(major, 0, 0);
    }

    @JsonCreator
    public Version(@JsonProperty("major") @Nonnull final Integer major,
                   @JsonProperty("minor") @Nonnull final Integer minor,
                   @JsonProperty("patch") @Nonnull final Integer patch)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public Version(@Nonnull final Integer major, @Nonnull final Integer minor)
    {
        this(major, minor, 0);
    }

    @Nonnull
    public Integer major() { return this.major; }

    @Nonnull
    public Integer minor() { return this.minor; }

    @Nonnull
    public Integer patch() { return this.patch; }

    @Nonnull
    public Version nextMajor() { return new Version(this.major + 1, 0, 0); }

    @Nonnull
    public Version nextMinor() { return new Version(this.major, this.minor + 1, 0); }

    @Nonnull
    public Version nextPatch() { return new Version(this.major, this.minor, this.patch + 1); }

    public boolean isBefore(@Nonnull final Version other)
    {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(@Nonnull final Version other)
    {
        return this.compareTo(other) > 0;
    }

    public boolean isBetween(@Nonnull final Version first, @Nonnull final Version last)
    {
        return this.compareTo(first) >= 0 && this.compareTo(last) < 0;
    }

    @Override
    public int compareTo(@Nonnull final Version o)
    {
        return NATURAL.compare(this, o);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.major, this.minor, this.patch);
    }

    @Override
    public boolean equals(final Object other)
    {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        final Version version = (Version) other;
        return Objects.equal(this.major, version.major) &&
        Objects.equal(this.minor, version.minor) &&
        Objects.equal(this.patch, version.patch);
    }

    @Override
    public String toString()
    {
        return CONVERTER.reverse().convert(this);
    }

    @Override
    public Version intern(@Nonnull final Version sample)
    {
        return INTERNER.intern(sample);
    }

    public static class Serializer extends JsonSerializer<Version>
    {
        @Override
        public void serialize(final Version value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException
        {
            gen.writeString(CONVERTER.reverse().convert(value));
        }
    }

    public static class Deserializer extends JsonDeserializer<Version>
    {
        @Override
        public Version deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException
        {
            return CONVERTER.convert(p.getText());
        }
    }

    public static class StringConverter extends Converter<String,Version>
    {
        private static final Pattern STRING_REPRESENTATION;

        static
        {
            STRING_REPRESENTATION = Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
        }

        private static enum Field
        {
            ENTIRE, // 0 ( entire match )
            MAJOR, // 1
            MINOR, // 2
            PATCH; // 3
        }

        @Override
        @SuppressWarnings("UnstableApiUsage")
        protected Version doForward(@Nonnull final String s)
        {
            final Matcher matcher = STRING_REPRESENTATION.matcher(s);
            if (matcher.matches())
            {
                final Integer major1 = checkNotNull(tryParse(matcher.group(Field.MAJOR.ordinal())));
                final Integer minor1 = checkNotNull(tryParse(matcher.group(Field.MINOR.ordinal())));
                final Integer patch1 = checkNotNull(tryParse(matcher.group(Field.PATCH.ordinal())));
                return new Version(major1, minor1, patch1);
            }
            else
            {
                throw new IllegalArgumentException(format("%s is not a valid formatted Version %s", s, STRING_REPRESENTATION.pattern()));
            }
        }

        @Override
        protected String doBackward(@Nonnull final Version version)
        {
            return format("%d.%d.%d", version.major, version.minor, version.patch);
        }
    }
}

